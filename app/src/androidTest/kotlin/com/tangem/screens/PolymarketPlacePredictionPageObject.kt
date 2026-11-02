package com.tangem.screens

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import com.tangem.common.BaseTestCase
import com.tangem.core.ui.test.PredictionTestTags
import io.github.kakaocup.compose.node.element.ComposeScreen
import io.github.kakaocup.compose.node.element.ComposeScreen.Companion.onComposeScreen
import io.github.kakaocup.compose.node.element.KNode

class PolymarketPlacePredictionPageObject(semanticsProvider: SemanticsNodeInteractionsProvider) :
    ComposeScreen<PolymarketPlacePredictionPageObject>(semanticsProvider = semanticsProvider) {

    val screen: KNode = child {
        hasTestTag(PredictionTestTags.PLACE_PREDICTION_SCREEN)
    }
}

internal fun BaseTestCase.onPolymarketPlacePredictionScreen(function: PolymarketPlacePredictionPageObject.() -> Unit) =
    onComposeScreen(composeTestRule, function)