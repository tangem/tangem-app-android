package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.YieldSupplyTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class YieldSupplyStartEarningPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyStartEarningPageObject>(semanticsProvider = semanticsProvider) {

    val startEarningButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.START_EARNING_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyStartEarningScreen(function: YieldSupplyStartEarningPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)