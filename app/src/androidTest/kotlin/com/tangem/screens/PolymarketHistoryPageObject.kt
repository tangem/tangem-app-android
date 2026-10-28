package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.PredictionTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class PolymarketHistoryPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PolymarketHistoryPageObject>(semanticsProvider = semanticsProvider) {

    val screen: KNode = child {
        hasTestTag(PredictionTestTags.HISTORY_SCREEN)
    }
}

internal fun BaseTestCase.onPolymarketHistoryScreen(function: PolymarketHistoryPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)