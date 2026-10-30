package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.PredictionTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class PolymarketPositionDetailsPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PolymarketPositionDetailsPageObject>(semanticsProvider = semanticsProvider) {

    val closePositionButton: KNode = child {
        hasTestTag(PredictionTestTags.POSITION_CLOSE_BUTTON)
    }

    val buyButton: KNode = child {
        hasTestTag(PredictionTestTags.POSITION_BUY_BUTTON)
    }
}

internal fun BaseTestCase.onPolymarketPositionDetailsScreen(function: PolymarketPositionDetailsPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)