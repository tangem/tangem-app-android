package com.tangem.features.polymarket.impl.placeprediction.status

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionRoute
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionStatusModel
import com.tangem.features.polymarket.impl.placeprediction.ui.PlacePredictionStatusContent

internal class PlacePredictionStatusComponent(
    appComponentContext: AppComponentContext,
    private val params: PlacePredictionRoute.Status,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PlacePredictionStatusModel = getOrCreateModel(params)

    @Composable
    override fun Content(modifier: Modifier) {
        val viewPosition by model.viewPosition.collectAsStateWithLifecycle()

        PlacePredictionStatusContent(
            result = params.result,
            viewPosition = viewPosition,
            market = params.market,
            side = params.side,
            intents = model,
            modifier = modifier,
        )
    }
}