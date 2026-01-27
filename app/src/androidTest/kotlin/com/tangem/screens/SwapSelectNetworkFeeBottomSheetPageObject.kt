package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.SelectNetworkFeeBottomSheetTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class SwapSelectNetworkFeeBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapSelectNetworkFeeBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasText(getResourceString(R.string.fee_selector_choose_speed_title))
        useUnmergedTree = true
    }

    val marketSelectorItem: KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_TITLE)
        hasText(getResourceString(R.string.common_fee_selector_option_market))
        useUnmergedTree = true
    }

    val fastSelectorItem: KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_TITLE)
        hasText(getResourceString(R.string.common_fee_selector_option_fast))
        useUnmergedTree = true
    }

    val readMoreTextBlock: KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.LEARN_MORE_TEXT)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSwapSelectNetworkFeeBottomSheet(function: SwapSelectNetworkFeeBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)