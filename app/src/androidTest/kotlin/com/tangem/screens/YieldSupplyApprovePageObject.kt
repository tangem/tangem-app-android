package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.YieldSupplyTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class YieldSupplyApprovePageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyApprovePageObject>(semanticsProvider = semanticsProvider) {

    val confirmButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.APPROVE_CONFIRM_BUTTON)
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyApproveScreen(function: YieldSupplyApprovePageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)