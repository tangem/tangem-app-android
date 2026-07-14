package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.YieldSupplyTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class YieldSupplyStopEarningPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyStopEarningPageObject>(semanticsProvider = semanticsProvider) {

    val confirmButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.STOP_EARNING_CONFIRM_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyStopEarningScreen(function: YieldSupplyStopEarningPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)