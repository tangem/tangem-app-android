package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.YieldSupplyTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class YieldSupplyPromoPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyPromoPageObject>(semanticsProvider = semanticsProvider) {

    val continueButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.PROMO_CONTINUE_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyPromoScreen(function: YieldSupplyPromoPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)