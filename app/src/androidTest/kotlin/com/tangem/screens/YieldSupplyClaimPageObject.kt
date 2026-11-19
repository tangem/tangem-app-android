package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.test.YieldSupplyTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString

class YieldSupplyClaimPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<YieldSupplyClaimPageObject>(semanticsProvider = semanticsProvider) {

    val confirmButton: KNode = child {
        hasTestTag(YieldSupplyTestTags.CLAIM_CONFIRM_BUTTON)
        useUnmergedTree = true
    }

    val sheetTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.yield_module_claim_sheet_title))
        useUnmergedTree = true
    }

    fun confirmButtonText(total: String): KNode = child {
        hasText(getResourceString(CoreResR.string.yield_module_claim_button_title, total))
        useUnmergedTree = true
    }
}

internal fun BaseTestCase.onYieldSupplyClaimScreen(function: YieldSupplyClaimPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)