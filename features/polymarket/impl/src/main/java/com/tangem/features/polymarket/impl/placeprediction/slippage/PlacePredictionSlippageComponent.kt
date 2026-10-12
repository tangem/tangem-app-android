package com.tangem.features.polymarket.impl.placeprediction.slippage

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSlippageModel
import com.tangem.features.polymarket.impl.placeprediction.ui.PlacePredictionSlippageContent

internal class PlacePredictionSlippageComponent(
    appComponentContext: AppComponentContext,
    params: PlacePredictionRoute.Slippage,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PlacePredictionSlippageModel = getOrCreateModel(params = params)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PlacePredictionSlippageContent(state = state, intents = model, modifier = modifier)
    }
}