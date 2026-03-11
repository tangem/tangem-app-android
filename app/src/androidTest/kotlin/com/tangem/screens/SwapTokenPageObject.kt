package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.R
import com.tangem.core.ui.test.*
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

class SwapTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapTokenPageObject>(semanticsProvider = semanticsProvider) {

    val container: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.CONTAINER)
    }

    val title: KNode = child {
        hasTestTag(TopAppBarTestTags.TITLE)
        hasText(getResourceString(R.string.common_swap))
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(TopAppBarTestTags.CLOSE_BUTTON)
    }

    val textInput: KNode = child {
        hasParent(withTestTag(SwapTokenScreenTestTags.SWAP_TEXT_FIELD))
        useUnmergedTree = true
    }

    val networkFeeBlock: KNode = child {
        hasTestTag(FeeSelectorBlockTestTags.SELECTOR_BLOCK)
        useUnmergedTree = true
    }

    val selectFeeIcon: KNode = child {
        hasTestTag(FeeSelectorBlockTestTags.SELECT_FEE_ICON)
        useUnmergedTree = true
    }

    val receiveAmountShimmer: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.RECEIVE_AMOUNT_SHIMMER)
    }

    val swapTokensOnscreenButton: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.SWAP_BUTTON)
    }

    val receiveAmount: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.RECEIVE_TEXT_FIELD)
        useUnmergedTree = true
    }

    val providersBlock: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.PROVIDERS_BLOCK)
        useUnmergedTree = true
    }

    val errorNotificationTitle: KNode = child {
        hasTestTag(NotificationTestTags.TITLE)
        useUnmergedTree = true
    }

    val errorNotificationText: KNode = child {
        hasTestTag(NotificationTestTags.MESSAGE)
        useUnmergedTree = true
    }

    val refreshButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.warning_button_refresh))
    }

    val swapButton: KNode = child {
        hasTestTag(BaseButtonTestTags.BUTTON)
        hasText(getResourceString(R.string.common_swap))
    }

    val youSwapBlock: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.SWAP_BLOCK_HEADER)
        hasAnyDescendant(withText(getResourceString(R.string.swapping_from_title)))
        hasAnyDescendant(withTestTag(SwapTokenScreenTestTags.BALANCE))
        useUnmergedTree = true
    }

    val youReceiveBlock: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.SWAP_BLOCK_HEADER)
        hasAnyDescendant(withText(getResourceString(R.string.swapping_to_title)))
        hasAnyDescendant(withTestTag(SwapTokenScreenTestTags.BALANCE))
        useUnmergedTree = true
    }

    val receiveFiatAmount: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.RECEIVE_FIAT_AMOUNT)
    }

    val swapFiatAmount: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.SWAP_FIAT_AMOUNT)
    }

    val changeTokenIcon: KNode = child {
        hasTestTag(SwapTokenScreenTestTags.SELECT_TOKEN_ICON)
    }

    fun swapTokenSymbol(symbol: String): KNode = child {
        hasAnyAncestor(withTestTag(SwapTokenScreenTestTags.SWAP_CARD))
        hasTestTag(SwapTokenScreenTestTags.TOKEN_SYMBOL)
        hasText(symbol)
        useUnmergedTree = true
    }

    fun receiveTokenSymbol(symbol: String): KNode = child {
        hasAnyAncestor(withTestTag(SwapTokenScreenTestTags.RECEIVE_CARD))
        hasTestTag(SwapTokenScreenTestTags.TOKEN_SYMBOL)
        hasText(symbol)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSwapTokenScreen(function: SwapTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)