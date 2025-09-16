package com.tangem.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyAncestor
import com.tangem.common.BaseTestCase
import com.tangem.common.extensions.hasLazyListItemPosition
import com.tangem.common.utils.LazyListItemNode
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.core.ui.test.NotificationTestTags
import com.tangem.core.ui.test.TokenElementsTestTags
import com.tangem.core.ui.utils.LazyListItemPositionSemantics
import com.tangem.feature.wallet.impl.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.compose.node.element.lazylist.KLazyListNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class MainScreenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
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
        hasTestTag(MainScreenTestTags.MULTI_CURRENCY_ACTION_BUTTON)
        hasText(getResourceString(R.string.common_buy))
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

    val seedPhraseNotificationIcon: KNode = child {
        hasAnySibling(withText(getResourceString(R.string.warning_seedphrase_issue_title)))
        hasTestTag(NotificationTestTags.ICON)
        useUnmergedTree = true
    }

    val seedPhraseNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        hasText(getResourceString(R.string.warning_seedphrase_issue_title))
        useUnmergedTree = true
    }

    val seedPhraseNotificationMessage: KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        hasText(getResourceString(R.string.warning_seedphrase_issue_message))
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
        hasParent(withTestTag(MainScreenTestTags.WALLET_BALANCE))
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

    /**
     * Find token list item with title and address
     */
    @OptIn(ExperimentalTestApi::class)
    fun tokenWithTitleAndAddress(tokenTitle: String): KNode {
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_FIAT_AMOUNT)
            useUnmergedTree = true
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun organizeTokensButton(): KNode {
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.ORGANIZE_TOKENS_BUTTON)
        }.child<KNode> {
            hasText(getResourceString(R.string.organize_tokens_title))
            useUnmergedTree = true
        }
    }

    fun tokenNetworkGroupTitle(tokenNetwork: String): KNode {
        return lazyList.child {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasAnyChild(withText(tokenNetwork))
            useUnmergedTree = true
        }
    }

    @OptIn(ExperimentalTestApi::class)
    fun tokenWithTitleAndPosition(tokenTitle: String, index: Int): KNode {
        return lazyList.childWith<LazyListItemNode> {
            hasTestTag(MainScreenTestTags.TOKEN_LIST_ITEM)
            hasText(tokenTitle)
            hasLazyListItemPosition(index)
        }.child<KNode> {
            hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
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
        try {
            tokenWithTitleAndAddress(tokenTitle).assertExists()
            throw AssertionError("Token with title '$tokenTitle' should not exist but was found")
        } catch (e: AssertionError) {
            if (e.message?.contains("No node found") == true) {
                return
            } else {
                throw e
            }
        }
    }
}

internal fun BaseTestCase.onMainScreen(function: MainScreenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)