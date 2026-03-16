package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.*
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasText as withText

class SwapSelectTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapSelectTokenPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.common_swap))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    val youSwapTitle: KNode = child {
        hasText(getResourceString(R.string.swapping_from_title))
        useUnmergedTree = true
    }

    val youSwapBlock: KNode = child {
        hasTestTag(SwapSelectTokenScreenTestTags.YOU_SWAP_BLOCK)
        hasAnyDescendant(withText(getResourceString(R.string.action_buttons_you_want_to_swap)))
        useUnmergedTree = true
    }

    val youReceiveTitle: KNode = child {
        hasText(getResourceString(R.string.swapping_to_title))
        useUnmergedTree = true
    }

    val youReceiveBlock: KNode = child {
        hasTestTag(SwapSelectTokenScreenTestTags.YOU_SWAP_BLOCK)
        hasAnyDescendant(withText(getResourceString(R.string.action_buttons_you_want_to_receive)))
        useUnmergedTree = true
    }

    val searchBarBlock: KNode = child {
        hasTestTag(BaseSearchBarTestTags.SEARCH_BAR)
        useUnmergedTree = true
    }

    val searchBarIcon: KNode = child {
        hasTestTag(SearchBarTestTags.ICON)
        useUnmergedTree = true
    }

    val searchBarPlaceholderText: KNode = child {
        hasTestTag(SearchBarTestTags.PLACEHOLDER_TEXT)
        useUnmergedTree = true
    }

    val tryAgainButton: KNode = child {
        hasText(getResourceString(R.string.alert_button_try_again))
        useUnmergedTree = true
    }

    val unableToLoadData: KNode = child {
        hasText(getResourceString(R.string.markets_loading_error_title))
        useUnmergedTree = true
    }

    fun tokenWithName(tokenName: String): KNode = child {
        hasTestTag(TokenElementsTestTags.TOKEN_TITLE)
        hasAnyChild(withText(tokenName))
        useUnmergedTree = true
    }

    fun marketsTokenWithName(title: String): KNode {
        return child {
            hasTestTag(MarketsTestTags.TOKENS_LIST_ITEM)
            hasText(title)
        }
    }
}

internal fun BaseTestCase.onSwapSelectTokenScreen(function: SwapSelectTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)