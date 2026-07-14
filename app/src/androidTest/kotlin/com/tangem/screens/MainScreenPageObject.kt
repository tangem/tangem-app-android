package com.tangem.screens

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.displayedTextsInVisualOrder
import com.tangem.common.extensions.firstTextOrNull
import com.tangem.common.extensions.getQuantityString
import com.tangem.common.extensions.hasLazyListItemPosition
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.*
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.feature.wallet.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import kotlin.math.abs
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

class MainScreenPageObject(private val semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<MainScreenPageObject>(semanticsProvider = semanticsProvider) {

    private val lazyList = KLazyListNode(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(MainScreenTestTags.SCREEN_CONTAINER) },
        itemTypeBuilder = {
            itemType(::LazyListItemNode)
        },
        positionMatcher = { position ->
            SemanticsMatcher.expectValue(
                LazyListItemPositionSemantics,
                position
            )
        }
    )

    val screenContainer: KNode = child {
        hasTestTag(MainScreenTestTags.SCREEN_CONTAINER)
    }

    val synchronizeAddressesButton: KNode = lazyList.child {
        hasText(getResourceString(R.string.common_generate_addresses))
    }

    val buyButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_buy)))
        useUnmergedTree = true
    }

    val addFundsButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_add_funds)))
        useUnmergedTree = true
    }

    val sendButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_send)))
        useUnmergedTree = true
    }

    val receiveButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_receive)))
        useUnmergedTree = true
    }

    val transferButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_transfer)))
        useUnmergedTree = true
    }

    val swapButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_swap)))
        useUnmergedTree = true
    }

    val walletNameText: KNode = child {
        hasTestTag(MainScreenTestTags.CARD_TITLE)
        useUnmergedTree = true
    }

    val walletImage: KNode = child {
        hasTestTag(MainScreenTestTags.CARD_IMAGE)
        useUnmergedTree = true
    }

    /**
     * Collapses the collapsing header via a touch-based swipe so that items near the bottom
     * of the lazy list fall within screen bounds before programmatic childWith scroll.
     * Required because TangemCollapsingTopBar places the body at y=collapsingHeight, which
     * pushes lower list items off-screen when the header is expanded.
     */
    fun collapseHeader() {
        screenContainer {
            performTouchInput { swipeUp(startY = visibleSize.height * 0.6f, endY = visibleSize.height * 0.1f) }
        }
    }

    /** Scrolls to [accountName] via ScrollToIndex semantics, not a touch swipe — a bottom-edge drag is stolen by the Markets sheet's nested scroll. */
    @OptIn(ExperimentalTestApi::class)
    fun scrollToAccount(accountName: String) {
        semanticsProvider.onNode(withTestTag(MainScreenTestTags.SCREEN_CONTAINER))
            .performScrollToNode(
                withTestTag(MainScreenTestTags.ACCOUNT_LIST_ITEM) and hasAnyDescendant(withText(accountName)),
            )
    }

    // Wallet pager keeps the adjacent page composed (beyondViewportPageCount=1), so the token is mounted on two pages — click the displayed copy.
    fun clickDisplayedToken(tokenName: String) {
        val matcher = withTestTag(MainScreenTestTags.TOKEN_LIST_ITEM) and hasAnyDescendant(withText(tokenName))
        val nodes = semanticsProvider.onAllNodes(matcher, useUnmergedTree = true)
        for (i in 0 until nodes.fetchSemanticsNodes().size) {
            if (runCatching { nodes[i].assertIsDisplayed(); nodes[i].performClick() }.isSuccess) return
        }
        error("Token '$tokenName' is not displayed on the current wallet page")
    }

    /**
     * Switches to the previous/next wallet in the pager.
     *
     * The pager only accepts horizontal swipes while the collapsing balance header is fully
     * expanded and pinned to the top (`canPagerScroll = heightOffset == 0`). A *partially* collapsed
     * header still shows the wallet card, yet the horizontal swipe is a silent no-op — so a visible
     * card is not a reliable "ready to page" signal. We therefore try the swipe, and whenever the
     * wallet identity doesn't change we expand the header and retry, so a no-op swipe can't pass
     * unnoticed. Identity is title+balance rather than just the name: a still-restoring wallet (hot
     * wallet) shows "Restoring…" instead of a title, but always shows a balance.
     */
    @OptIn(ExperimentalTestApi::class)
    fun swipeToAdjacentWallet(toPrevious: Boolean) {
        val before = displayedWalletIdentity()
        repeat(times = WALLET_SWITCH_ATTEMPTS) {
            // Swipe first. The pager only pages while the header is pinned to the top; if it already
            // is (the common case), a swipe-down here would instead trigger pull-to-refresh and
            // un-pin it, breaking the horizontal swipe — so we only expand *after* a swipe that
            // didn't page (i.e. the header was collapsed).
            swipeCurrentPage(toPrevious)
            val now = displayedWalletIdentity()
            if (now != null && now != before) return
            expandCollapsingHeader()
        }
        error("Wallet did not switch from '$before' after $WALLET_SWITCH_ATTEMPTS attempts")
    }

    /** Expands the collapsing balance header (pins the balance to the top) so the pager can page. */
    @OptIn(ExperimentalTestApi::class)
    private fun expandCollapsingHeader() {
        onScreenPageNode(withTestTag(MainScreenTestTags.SCREEN_CONTAINER))?.performTouchInput {
            swipeDown(startY = visibleSize.height * 0.3f, endY = visibleSize.height * 0.8f)
        }
    }

    /**
     * Pages to the previous/next wallet by swiping horizontally on the on-screen token list.
     * The list is a vertical scroller, so it passes the horizontal drag up to the HorizontalPager
     * instead of consuming it (a per-page balance card may consume horizontal gestures itself).
     */
    @OptIn(ExperimentalTestApi::class)
    private fun swipeCurrentPage(toPrevious: Boolean) {
        onScreenPageNode(withTestTag(MainScreenTestTags.SCREEN_CONTAINER))?.performTouchInput {
            if (toPrevious) swipeRight() else swipeLeft()
        }
    }

    /**
     * A best-effort identity of the on-screen wallet: its title combined with its balance. Works in
     * every sync state — a still-restoring wallet has no [MainScreenTestTags.CARD_TITLE] but always
     * shows a [MainScreenTestTags.WALLET_BALANCE] — so the value reliably differs across a swap.
     */
    private fun displayedWalletIdentity(): String? {
        val page = onScreenPageRect() ?: return null
        val title = firstTextInPage(page, MainScreenTestTags.CARD_TITLE)
        val balance = firstTextInPage(page, MainScreenTestTags.WALLET_BALANCE)
        return listOfNotNull(title, balance).joinToString(separator = "|").ifBlank { null }
    }

    /**
     * Horizontal bounds of the on-screen wallet page, taken from its token list. SCREEN_CONTAINER is
     * reliably one-per-page and full-width (unlike WALLET_LIST_ITEM, which also tags a multi-page
     * wrapper), so it is the trustworthy anchor for "which page is on-screen".
     */
    private fun onScreenPageRect(): Rect? =
        onScreenPageNode(withTestTag(MainScreenTestTags.SCREEN_CONTAINER))?.fetchSemanticsNode()?.boundsInRoot

    /** First text of the node tagged [tag] whose centre lies within the on-screen [page]. */
    private fun firstTextInPage(page: Rect, tag: String): String? {
        val nodes = semanticsProvider.onAllNodes(withTestTag(tag), useUnmergedTree = true)
        for (i in 0 until nodes.fetchSemanticsNodes().size) {
            val text = runCatching {
                val bounds = nodes[i].fetchSemanticsNode().boundsInRoot
                // Skip zero-size nodes: off-screen pager pages collapse to bounds (0,0,0,0), whose
                // centre (0,0) would otherwise pass the "centre within page" test (page.left is 0)
                // and leak a stale wallet's text — the exact cause of undetected wallet switches.
                if (bounds.width > 0f && bounds.height > 0f &&
                    bounds.center.x >= page.left && bounds.center.x < page.right
                ) {
                    nodes[i].fetchSemanticsNode().firstTextOrNull()
                } else {
                    null
                }
            }.getOrNull()
            if (text != null) return text
        }
        return null
    }

    /**
     * The full-width pager-page node matching [matcher] that is currently centred on-screen.
     *
     * The wallet pager keeps adjacent pages composed (beyondViewportPageCount=1) at ±pageWidth, so
     * `onAllNodes` returns nodes from off-screen pages too. `assertIsDisplayed` is unreliable here
     * (off-screen pages are placed, not clipped away), so we select by geometry: the on-screen page
     * is the only full-width node whose left edge is within half a page of x=0.
     */
    private fun onScreenPageNode(matcher: SemanticsMatcher): SemanticsNodeInteraction? {
        val nodes = semanticsProvider.onAllNodes(matcher)
        for (i in 0 until nodes.fetchSemanticsNodes().size) {
            val node = nodes[i]
            val onScreen = runCatching {
                val bounds = node.fetchSemanticsNode().boundsInRoot
                abs(bounds.left) < bounds.width / 2f
            }.getOrDefault(false)
            if (onScreen) return node
        }
        return null
    }

    val restoringProgressText: KNode = child {
        hasTestTag(MainScreenTestTags.SYNC_PROGRESS_TEXT)
        useUnmergedTree = true
    }

    val walletImportedBanner: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.initial_wallet_sync_banner_title))
        useUnmergedTree = true
    }

    val walletImportedBannerCheckHereButton: KNode = child {
        hasAnyAncestor(withTestTag(NotificationTestTags.CONTAINER))
        hasText(getResourceString(CoreResR.string.main_manage_tokens))
        useUnmergedTree = true
    }

    @OptIn(ExperimentalTestApi::class)
    fun marketPriceBlock(): LazyListItemNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MarketPriceBlockTestTags.BLOCK)
            useUnmergedTree = true
        }
    }

    val marketPriceText: KNode = child {
        hasTestTag(MarketPriceBlockTestTags.TEXT)
        useUnmergedTree = true
    }

    val transactionsExplorerIcon: KNode = child {
        hasTestTag(TransactionHistoryBlockTestTags.EXPLORER_ICON)
        useUnmergedTree = true
    }

    val transactionsTitle: KNode = child {
        hasTestTag(TransactionHistoryBlockTestTags.TITLE_TEXT)
        hasText(getResourceString(R.string.common_transactions))
        useUnmergedTree = true
    }

    val transactionsExplorerText: KNode = child {
        hasTestTag(TransactionHistoryBlockTestTags.EXPLORER_TEXT)
        hasText(getResourceString(R.string.common_explorer))
        useUnmergedTree = true
    }

    val emptyTransactionBlock: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.BLOCK)
    }

    val emptyTransactionBlockIcon: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.ICON)
    }

    val emptyTransactionBlockText: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.TEXT)
    }

    val emptyTransactionBlockExploreButton: KNode = child {
        hasTestTag(EmptyTransactionBlockTestTags.EXPLORE_BUTTON)
    }

    val notificationContainer: KNode = child {
        hasTestTag(NotificationTestTags.CONTAINER)
        useUnmergedTree = true
    }

    val getTangemPayBanner: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(CoreResR.string.tangempay_onboarding_banner_title))
        useUnmergedTree = true
    }

    val devCardNotificationIcon: KNode = child {
        hasAnySibling(withText(getResourceString(R.string.warning_developer_card_title)))
        hasTestTag(NotificationTestTags.ICON)
        useUnmergedTree = true
    }

    val devCardNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.warning_developer_card_title))
        useUnmergedTree = true
    }

    val devCardNotificationMessage: KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(getResourceString(R.string.warning_developer_card_message))
        useUnmergedTree = true
    }

    val missingAddressNotificationIcon: KNode = child {
        hasAnySibling(withText(getResourceString(R.string.warning_missing_derivation_title)))
        hasTestTag(NotificationTestTags.ICON)
        useUnmergedTree = true
    }

    val missingAddressNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.warning_missing_derivation_title))
        useUnmergedTree = true
    }

    fun missingAddressNotificationMessage(networkCount: Int): KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(
            getQuantityString(
                R.plurals.warning_missing_derivation_message,
                networkCount,
                networkCount
            )
        )
        useUnmergedTree = true
    }

    val totalBalanceContainer: KNode = child {
        hasTestTag(MainScreenTestTags.WALLET_LIST_ITEM)
    }

    val totalBalanceMenuRenameWallet: KNode = child {
        hasTestTag(MainScreenTestTags.TOTAL_BALANCE_MENU_ITEM)
        hasText(getResourceString(R.string.common_rename))
    }

    val totalBalanceMenuDeleteWallet: KNode = child {
        hasTestTag(MainScreenTestTags.TOTAL_BALANCE_MENU_ITEM)
        hasText(getResourceString(R.string.common_delete))
    }

    val totalBalanceText: KNode = child {
        hasAnyAncestor(withTestTag(MainScreenTestTags.WALLET_BALANCE))
        addSemanticsMatcher(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
    }

    val notificationYesButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_yes))
        useUnmergedTree = true
    }

    val notificationNoButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_no))
        useUnmergedTree = true
    }

    val snackbarCopiedAddressMessage: KNode = child {
        hasText(getResourceString(CoreUiR.string.wallet_notification_address_copied))
    }

    val addAndManageButtonNode: KNode = child {
        hasTestTag(MainScreenTestTags.ADD_AND_MANAGE_BUTTON)
        useUnmergedTree = true
    }

    /**
     * Empty-tokens placeholder shown under an expanded account that has no tokens.
     */
    val emptyAccountTokensPlaceholder: KNode = child {
        hasTestTag(MainScreenTestTags.EMPTY_TOKENS_PLACEHOLDER)
        useUnmergedTree = true
    }

    /**
     * 'Add tokens' button inside the empty-account placeholder. Click opens manage tokens for that account.
     */
    val emptyAccountAddTokensButton: KNode = child {
        hasTestTag(MainScreenTestTags.EMPTY_TOKENS_ADD_BUTTON)
        useUnmergedTree = true
    }

    /**
     * Main account header on the main screen. Click to expand/collapse its tokens list.
     */
    fun mainAccount(): LazyListItemNode = accountWithName(getResourceString(CoreUiR.string.account_main_account_title))

    /**
     * Account header on the main screen, located by its visible name. Click to expand/collapse its tokens list.
     * The account's title text lives on a descendant of the test-tagged node, so we match by descendant.
     */
    @OptIn(ExperimentalTestApi::class)
    fun accountWithName(name: String): LazyListItemNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyDescendant(withText(name))
            useUnmergedTree = true
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun tokenRowWithTitle(tokenTitle: String): LazyListItemNode {
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
            useUnmergedTree = true
        }
    }

    /**
     * Find token list item with title and address
     */
    @OptIn(ExperimentalTestApi::class)
    fun tokenWithTitleAndAddress(tokenTitle: String): KNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
            useUnmergedTree = true
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT)
            useUnmergedTree = true
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun tokenWithCustomDerivationIcon(tokenTitle: String): KNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
            useUnmergedTree = true
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_CUSTOM_DERIVATION_ICON)
            useUnmergedTree = true
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun addAndManageButton(): KNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.ADD_AND_MANAGE_BUTTON)
        }.child<KNode> {
            hasText(getResourceString(CoreResR.string.main_add_and_manage_tokens))
            useUnmergedTree = true
        }
    }

    val addAndManageButtonWithoutLazySearch: KNode = child {
        hasTestTag(MainScreenTestTags.ADD_AND_MANAGE_BUTTON)
        hasText(getResourceString(CoreResR.string.main_add_and_manage_tokens))
        useUnmergedTree = true
    }

    /**
     * Collapses the header, scrolls to and clicks the 'Add & manage' button on the wallet page
     * that is actually on-screen.
     *
     * The wallet pager keeps the adjacent page composed (beyondViewportPageCount=1) at ±pageWidth,
     * so both pages' MAIN_SCREEN_CONTAINER / button nodes are in the tree at once. We select the
     * on-screen page by geometry (see [onScreenPageNode]) and click only the button within it.
     */
    @OptIn(ExperimentalTestApi::class)
    fun clickDisplayedAddAndManageButton() {
        val container = onScreenPageNode(withTestTag(MainScreenTestTags.SCREEN_CONTAINER))
            ?: error("No on-screen wallet page found")
        // Best-effort: collapse the header and scroll the button into view. The button is a footer
        // outside the scrollable list, so performScrollToNode can throw — harmless when it's already
        // visible, but it must not abort the click below.
        runCatching {
            container.performTouchInput {
                swipeUp(startY = visibleSize.height * 0.6f, endY = visibleSize.height * 0.1f)
            }
            container.performScrollToNode(withTestTag(MainScreenTestTags.ADD_AND_MANAGE_BUTTON))
        }

        val page = container.fetchSemanticsNode().boundsInRoot
        val buttons = semanticsProvider.onAllNodes(
            withTestTag(MainScreenTestTags.ADD_AND_MANAGE_BUTTON),
            useUnmergedTree = true,
        )
        for (i in 0 until buttons.fetchSemanticsNodes().size) {
            val clicked = runCatching {
                val bounds = buttons[i].fetchSemanticsNode().boundsInRoot
                // Skip zero-size (off-screen page) buttons and require the centre within the page.
                if (bounds.width > 0f && bounds.height > 0f &&
                    bounds.center.x >= page.left && bounds.center.x < page.right
                ) {
                    buttons[i].performClick()
                    true
                } else {
                    false
                }
            }.getOrDefault(false)
            if (clicked) return
        }
        error("'Add & manage' button is not displayed on the current wallet page")
    }

    val searchThroughMarketPlaceholder: KNode = child {
        hasText(getResourceString(R.string.markets_search_title_placeholder))
        useUnmergedTree = true
    }

    val marketsSheetDragHandle: KNode = child {
        hasTestTag(MainScreenTestTags.MARKETS_SHEET_DRAG_HANDLE)
        useUnmergedTree = true
    }

    fun tokenNetworkGroupTitle(tokenNetwork: String): KNode {
        collapseHeader()
        return lazyList.child {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyChild(withText(tokenNetwork))
            useUnmergedTree = true
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun tokenWithTitleAndPosition(tokenTitle: String, index: Int): KNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
            hasLazyListItemPosition(index)
            useUnmergedTree = true
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
            useUnmergedTree = true
        }
    }

    /**
     * Account row on the main screen. Tappable — click to expand/collapse its tokens.
     */
    @OptIn(ExperimentalTestApi::class)
    fun findAccountSectionByName(accountName: String): KNode {
        return lazyList.child {
            hasTestTag(MainScreenTestTags.ACCOUNT_LIST_ITEM)
            hasAnyDescendant(withText(accountName))
            useUnmergedTree = true
        }
    }

    /**
     * Scrolls the account row into view and collapses the top bar so the account's tokens (or the
     * empty placeholder) land within screen bounds after expansion. Click via [findAccountSectionByName].
     */
    @OptIn(ExperimentalTestApi::class)
    fun scrollToAccountSection(accountName: String) {
        collapseHeader()
        lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.ACCOUNT_LIST_ITEM)
            hasAnyDescendant(withText(accountName))
            useUnmergedTree = true
        }
    }

    /**
     * Find a token row on the main screen by token name. Tokens belonging to collapsed accounts
     * are hidden from the semantics tree, so expanding a single account before calling this
     * effectively scopes the lookup to that account's tokens.
     */
    @OptIn(ExperimentalTestApi::class)
    fun findTokenInAnyAccountByName(tokenName: String): KNode {
        return lazyList.child {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyDescendant(withText(tokenName))
            useUnmergedTree = true
        }
    }

    fun KNode.assertIsUnreachable() {
        this {
            hasAnyAncestor(withText(getResourceString(R.string.common_unreachable)))
            assertIsDisplayed()
        }
    }

    /**
     * This assertion is required to properly verify the token's absence in the semantic tree.
     * Tests will fail if assertIsNotDisplayed() or assertDoesNotExist() are used instead.
     */
    fun assertTokenDoesNotExist(tokenTitle: String) {
        lazyList.child<KNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyDescendant(withText(tokenTitle))
            useUnmergedTree = true
        }.assertDoesNotExist()
    }

    fun assertTokensCount(expectedCount: Int) {
        semanticsProvider
            .onAllNodes(withTestTag(MainScreenTestTags.TOKEN_LIST_ITEM), useUnmergedTree = true)
            .assertCountEquals(expectedCount)
    }

    fun assertTokenExists(tokenTitle: String) {
        lazyList.child<KNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyDescendant(withText(tokenTitle))
            useUnmergedTree = true
        }.assertIsDisplayed()
    }

    /**
     * Token titles displayed on the current wallet page, in visual order. Reads the
     * [TokenElementsTestTags.TOKEN_TITLE] rows so network group headers (which share the
     * TOKEN_LIST_ITEM tag) are excluded, keeping the result symmetric with the 'Organize tokens' reader.
     */
    fun getDisplayedTokenTitles(): List<String> =
        semanticsProvider.onAllNodes(
            withTestTag(TokenElementsTestTags.TOKEN_TITLE),
            useUnmergedTree = true,
        ).displayedTextsInVisualOrder()

    private companion object {
        const val WALLET_SWITCH_ATTEMPTS = 4
    }
}

internal fun BaseTestCase.onMainScreen(function: MainScreenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)