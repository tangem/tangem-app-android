package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.BaseBottomSheetTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

class SwapSlippageBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<SwapSlippageBottomSheetPageObject>(
        semanticsProvider = semanticsProvider,
        viewBuilderAction = { hasTestTag(BaseBottomSheetTestTags.CONTAINER) },
    ) {

    val title: KNode = child {
        hasText(getResourceString(CoreResR.string.common_slippage))
        useUnmergedTree = true
    }

    val confirmButton: KNode = child {
        hasText(getResourceString(CoreResR.string.common_confirm))
        useUnmergedTree = true
    }

    fun providerSlippageInfo(slippagePercent: String): KNode = child {
        hasText(getResourceString(CoreResR.string.swap_custom_slippage_info, slippagePercent))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onSwapSlippageBottomSheet(function: SwapSlippageBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)