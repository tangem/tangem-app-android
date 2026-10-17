package com.tangem.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasAnyDescendant
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseButtonTestTags
import com.tangem.core.ui.test.SelectNetworkFeeBottomSheetTestTags
import com.tangem.wallet.R
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import androidx.compose.ui.test.hasTestTag as withTestTag
import androidx.compose.ui.test.hasText as withText

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

    val marketFeeItem: KNode = feeItem(getResourceString(R.string.common_fee_selector_option_market))

    val fastFeeItem: KNode = feeItem(getResourceString(R.string.common_fee_selector_option_fast))

    val marketFeeTokenAmount: KNode = feeItemTokenAmount(getResourceString(R.string.common_fee_selector_option_market))

    val fastFeeTokenAmount: KNode = feeItemTokenAmount(getResourceString(R.string.common_fee_selector_option_fast))

    private fun feeItem(title: String): KNode = child {
        addSemanticsMatcher(feeItemMatcher(title))
        useUnmergedTree = true
    }

    private fun feeItemTokenAmount(title: String): KNode = child {
        hasParent(withTestTag(SelectNetworkFeeBottomSheetTestTags.TOKEN_AMOUNT))
        hasAnyAncestor(feeItemMatcher(title))
        useUnmergedTree = true
    }

    val readMoreTextBlock: KNode = child {
        hasTestTag(SelectNetworkFeeBottomSheetTestTags.LEARN_MORE_TEXT)
        useUnmergedTree = true
    }

    val applyButton: KNode = child {
        hasTestTag(BaseButtonTestTags.TEXT)
        hasText(getResourceString(R.string.common_apply))
        useUnmergedTree = true
    }
}

private fun feeItemMatcher(title: String): SemanticsMatcher =
    SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected) and
        hasAnyDescendant(withTestTag(SelectNetworkFeeBottomSheetTestTags.REGULAR_ITEM_TITLE) and withText(title))

internal fun BaseTestCase.onSwapSelectNetworkFeeBottomSheet(function: SwapSelectNetworkFeeBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)