package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.YieldSupplyTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class YieldModePromoBottomSheetPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldModePromoBottomSheetPageObject>(semanticsProvider = semanticsProvider) {

    val title: KNode = child {
        hasTestTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_TITLE)
        useUnmergedTree = true
    }

    val closeButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_CLOSE_BUTTON)
        useUnmergedTree = true
    }

    val tryButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.PROMO_BOTTOM_SHEET_TRY_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldModePromoBottomSheet(function: YieldModePromoBottomSheetPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)