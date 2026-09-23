package com.tangem.features.polymarket.impl.placeprediction.amount

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.essenty.lifecycle.doOnPause
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionAmountModel
import com.tangem.features.polymarket.impl.placeprediction.ui.PlacePredictionAmountContent

internal class PlacePredictionAmountComponent(
    appComponentContext: AppComponentContext,
    params: PlacePredictionComponent.Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PlacePredictionAmountModel = getOrCreateModel(params = params)

    init {
        lifecycle.doOnResume { model.onResume() }
        lifecycle.doOnPause { model.onPause() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()

        PlacePredictionAmountContent(state = state, intents = model, modifier = modifier)
    }
}