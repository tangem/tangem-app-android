package com.tangem.screens

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.displayedTextsInVisualOrder
import com.tangem.common.extensions.firstTextForTestTag
import com.tangem.common.extensions.firstTextOrNull
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

    @OptIn(ExperimentalTestApi::class)
    fun scrollToTokenList() {
        collapseHeader()
        semanticsProvider.onNode(withTestTag(MainScreenTestTags.SCREEN_CONTAINER))
            .performScrollToNode(withTestTag(MainScreenTestTags.TOKEN_LIST_ITEM))
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
     * Switches to the previous/next wallet in the pager. A horizontal swipe is a silent no-op unless
     * the collapsing balance header is fully expanded and pinned to the top, so we retry: swipe, and
     * whenever the wallet identity doesn't change, expand the header and try again.
     */
    fun swipeToAdjacentWallet(toPrevious: Boolean) {
        val before = displayedWalletIdentity()
        repeat(times = WALLET_SWITCH_ATTEMPTS) {
            swipeCurrentPage(toPrevious)
            val now = displayedWalletIdentity()
            if (now != null && now != before) return
            expandCollapsingHeader()
        }
        error("Wallet did not switch from '$before' after $WALLET_SWITCH_ATTEMPTS attempts")
    }

    // Expand only *after* a swipe that didn't page: if the header is already pinned, a swipe-down here
    // would trigger pull-to-refresh and un-pin it, breaking the horizontal swipe.
    private fun expandCollapsingHeader() {
        onScreenPage()?.performTouchInput {
            swipeDown(startY = visibleSize.height * 0.3f, endY = visibleSize.height * 0.8f)
        }
    }

    private fun swipeCurrentPage(toPrevious: Boolean) {
        onScreenPage()?.performTouchInput { if (toPrevious) swipeRight() else swipeLeft() }
    }

    // Identity = title + balance: a still-restoring wallet has no CARD_TITLE but always a WALLET_BALANCE.
    private fun displayedWalletIdentity(): String? {
        val title = onScreenPageChild(withTestTag(MainScreenTestTags.CARD_TITLE))?.firstText()
        val balance = onScreenPageChild(withTestTag(MainScreenTestTags.WALLET_BALANCE))?.firstText()
        return listOfNotNull(title, balance).joinToString(separator = "|").ifBlank { null }
    }

    /**
     * The full-width pager-page container currently on-screen. The pager keeps adjacent pages composed
     * off-screen at ±pageWidth (so [assertIsDisplayed] can't tell them apart), hence selection by
     * geometry: the on-screen page is the only one whose left edge is within half a page of x=0.
     */
    private fun onScreenPage(): SemanticsNodeInteraction? =
        firstNodeMatching(withTestTag(MainScreenTestTags.SCREEN_CONTAINER), useUnmergedTree = false) {
            abs(it.left) < it.width / 2f
        }

    /** A node matching [matcher] whose centre lies within the on-screen page (skips zero-size off-screen copies). */
    private fun onScreenPageChild(matcher: SemanticsMatcher): SemanticsNodeInteraction? {
        val page = onScreenPage()?.fetchSemanticsNode()?.boundsInRoot ?: return null
        return firstNodeMatching(matcher) {
            it.width > 0f && it.height > 0f && it.center.x >= page.left && it.center.x < page.right
        }
    }

    private fun SemanticsNodeInteraction.firstText(): String? = fetchSemanticsNode().firstTextOrNull()

    // Single geometry primitive behind the pager helpers: the first node matching [matcher] whose
    // bounds satisfy [predicate]. Replaces the per-caller onAllNodes(...)[i] loops.
    private fun firstNodeMatching(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean = true,
        predicate: (Rect) -> Boolean,
    ): SemanticsNodeInteraction? {
        val nodes = semanticsProvider.onAllNodes(matcher, useUnmergedTree = useUnmergedTree)
        repeat(times = nodes.fetchSemanticsNodes().size) { index ->
            val node = nodes[index]
            val matches = runCatching { predicate(node.fetchSemanticsNode().boundsInRoot) }.getOrDefault(false)
            if (matches) return node
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

    // The message is pluralised over the underived-network count, which varies by card.
    val missingAddressNotificationMessage: KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasAnyAncestor(
            withTestTag(NotificationTestTags.CONTAINER)
                .and(
                    androidx.compose.ui.test.hasAnyDescendant(
                        withText(getResourceString(R.string.warning_missing_derivation_title))
                    )
                )
        )
        useUnmergedTree = true
    }

    val totalBalanceContainer: KNode = child {
        hasTestTag(MainScreenTestTags.WALLET_LIST_ITEM)
    }

    val totalBalanceShimmer: KNode = child {
        hasTestTag(MainScreenTestTags.WALLET_BALANCE_SHIMMER)
        useUnmergedTree = true
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
     * 'Earn' APY/APR badge shown on a token row (staking or yield-supply indicator). Present only
     * when the token has an earn rate to display.
     */
    @OptIn(ExperimentalTestApi::class)
    fun tokenEarnApyBadge(tokenTitle: String): KNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
            useUnmergedTree = true
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_EARN_APY_BADGE)
        }
    }

    /**
     * Fiat-amount text of a token row (the balance shown on the top-right of the row). The tagged
     * container is a plain Row that merges into the clickable row, so we read its inner balance
     * Text node from the unmerged tree.
     */
    @OptIn(ExperimentalTestApi::class)
    fun tokenFiatAmountText(tokenTitle: String): KNode {
        collapseHeader()
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
            useUnmergedTree = true
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT)
            useUnmergedTree = true
        }.child<KNode> {
            addSemanticsMatcher(SemanticsMatcher.keyIsDefined(SemanticsProperties.Text))
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

    /** Collapses the header, scrolls to and clicks the 'Add & manage' button on the on-screen wallet page. */
    @OptIn(ExperimentalTestApi::class)
    fun clickDisplayedAddAndManageButton() {
        val container = onScreenPage() ?: error("No on-screen wallet page found")
        // Best-effort: the button is a footer outside the scrollable list, so performScrollToNode can
        // throw when it's already visible — that must not abort the click below.
        runCatching {
            container.performTouchInput {
                swipeUp(startY = visibleSize.height * 0.6f, endY = visibleSize.height * 0.1f)
            }
            container.performScrollToNode(withTestTag(MainScreenTestTags.ADD_AND_MANAGE_BUTTON))
        }
        val button = onScreenPageChild(withTestTag(MainScreenTestTags.ADD_AND_MANAGE_BUTTON))
            ?: error("'Add & manage' button is not displayed on the current wallet page")
        button.performClick()
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

    /**
     * (title to fiat amount) for every token row currently composed on the wallet page. A coin held on
     * several derivations renders one row per occurrence, so a title may repeat; a custom token with no
     * quote yields the dash sign as its fiat amount. Network-group header rows carry the same
     * [MainScreenTestTags.TOKEN_LIST_ITEM] tag but have no fiat amount, so they're skipped.
     *
     * onAllNodes escape hatch (same pattern as [getDisplayedTokenTitles]): the row is a merged-semantics
     * node — its title/fiat texts collapse into it, so a `childWith { hasAnyDescendant(text) }` can't reach
     * them and duplicate titles make `childWith` ambiguous. Reading the unmerged subtree per row pairs
     * title and fiat reliably.
     */
    fun getDisplayedTokenBalances(): List<Pair<String, String>> {
        collapseHeader()
        val rows = semanticsProvider.onAllNodes(withTestTag(MainScreenTestTags.TOKEN_LIST_ITEM), useUnmergedTree = true)
        val rowNodes = rows.fetchSemanticsNodes()
        return buildList {
            rowNodes.forEach { row ->
                val title = row.firstTextForTestTag(TokenElementsTestTags.TOKEN_TITLE) ?: return@forEach
                val fiatAmount = row.firstTextForTestTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT) ?: return@forEach
                add(title to fiatAmount)
            }
        }
    }

    /**
     * Asserts the token context menu (opened by a long-tap on a token row) shows every action in
     * [expectedActionIds]. When [exact] is `true` it additionally asserts the menu shows *no other* actions
     * (count matches exactly); when `false` it only checks the expected ones are present and tolerates extra
     * actions.
     *
     * The context menu renders inside a separate Popup root, i.e. NOT under [MainScreenTestTags.SCREEN_CONTAINER]
     * — that's why its rows are absent from `onMainScreen`'s subtree / a plain semantic-tree dump. So nodes are
     * matched globally via [semanticsProvider] (which spans every root, including popups) with
     * `useUnmergedTree` (each row merges its label/icon).
     *
     * Usage:
     * - [DERIVED_TOKEN_ACTIONS] with `exact = false` — a fully-derived token always exposes at least this
     *   stable subset, but the full set (e.g. 'Buy'/'Sell') varies per token/environment, so we assert
     *   presence only.
     * - [UNDERIVED_TOKEN_ACTIONS] with `exact = true` — a token without addresses exposes *only* 'Hide token',
     *   so the exact count matters.
     */
    fun assertTokenContextMenuActions(expectedActionIds: List<String>, exact: Boolean = true) {
        expectedActionIds.forEach { actionId ->
            semanticsProvider
                .onNode(withTestTag(TokenActionMenuTestTags.action(actionId)), useUnmergedTree = true)
                .assertIsDisplayed()
        }
        if (exact) {
            // Exactly the expected actions are shown — no more, no fewer.
            semanticsProvider
                .onAllNodes(withTestTag(BaseBottomSheetTestTags.ACTION_BUTTON), useUnmergedTree = true)
                .assertCountEquals(expectedActionIds.size)
        }
    }

    private companion object {
        const val WALLET_SWITCH_ATTEMPTS = 4
    }
}

/**
 * Context-menu actions a fully-derived token (with a resolved address) always exposes on the mocked
 * environment. This is a stable subset asserted for presence only (see [assertTokenContextMenuActions] with
 * `exact = false`) — the full set may additionally include environment-dependent actions like 'Buy'/'Sell'.
 */
internal val DERIVED_TOKEN_ACTIONS = listOf(
    TokenActionMenuTestTags.ANALYTICS,
    TokenActionMenuTestTags.COPY_ADDRESS,
    TokenActionMenuTestTags.RECEIVE,
    TokenActionMenuTestTags.HIDE_TOKEN,
    TokenActionMenuTestTags.SEND,
    TokenActionMenuTestTags.SWAP,
)

/** Context-menu actions expected for an underived token (no address) — only hiding is available. */
internal val UNDERIVED_TOKEN_ACTIONS = listOf(TokenActionMenuTestTags.HIDE_TOKEN)

internal fun BaseTestCase.onMainScreen(function: MainScreenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)