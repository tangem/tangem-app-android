package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class YieldSupplyClaimResultPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyClaimResultPageObject>(semanticsProvider = semanticsProvider) {

    val inProgressTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.yield_module_claim_in_progress_title), substring = true)
        useUnmergedTree = true
    }

    val goToYieldButton: KNode = child {
        hasText(getResourceString(CoreResR.string.yield_module_claim_go_to_yield_button))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyClaimResultScreen(function: YieldSupplyClaimResultPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)