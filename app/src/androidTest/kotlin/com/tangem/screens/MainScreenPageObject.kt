package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import com.tangem.common.BaseTestCase
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

    val sellButton: KNode = child {
        hasTestTag(BaseActionButtonsBlockTestTags.ACTION_BUTTON)
        hasAnyDescendant(withText(getResourceString(R.string.common_sell)))
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
    private fun collapseHeader() {
        screenContainer {
            performTouchInput { swipeUp(startY = visibleSize.height * 0.6f, endY = visibleSize.height * 0.1f) }
        }
    }

    val restoringProgressText: KNode = child {
        hasTestTag(MainScreenTestTags.SYNC_PROGRESS_TEXT)
        useUnmergedTree = true
    }

    val walletImportedBanner: KNode = child {
        hasTestTag(WalletNotificationTestTags.ASSETS_DISCOVERY_BANNER)
        useUnmergedTree = true
    }

    val walletImportedBannerCheckHereButton: KNode = child {
        hasAnyAncestor(withTestTag(WalletNotificationTestTags.ASSETS_DISCOVERY_BANNER))
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
}

internal fun BaseTestCase.onMainScreen(function: MainScreenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)