package com.tangem.features.polymarket.impl.placeprediction.slippage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionModel
import com.tangem.features.polymarket.impl.placeprediction.ui.PlacePredictionSlippageContent

internal class PlacePredictionSlippageComponent(
    appComponentContext: AppComponentContext,
    private val model: PlacePredictionModel,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PlacePredictionSlippageContent(state = state, intents = model, modifier = modifier)
    }
}