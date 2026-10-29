package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode
import io.github.kakaocup.kakao.common.utilities.getResourceString
import com.tangem.core.res.R as CoreResR

class PolymarketWithdrawPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PolymarketWithdrawPageObject>(semanticsProvider = semanticsProvider) {

    val sendFromTitle: KNode = child {
        hasText(getResourceString(CoreResR.string.swapping_from_account_title), substring = true)
    }
}

internal fun BaseTestCase.onPolymarketWithdrawScreen(function: PolymarketWithdrawPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)