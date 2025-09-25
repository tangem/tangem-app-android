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

class SwapTokenPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapTokenPageObject>(semanticsProvider = semanticsProvider) {

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
        hasTestTag(BaseBlockTestTags.BLOCK)
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

}

internal fun BaseTestCase.onSwapTokenScreen(function: SwapTokenPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)