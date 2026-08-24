package com.tangem.features.polymarket.impl.placeprediction.amount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.placeprediction.PlacePredictionComponent

internal class PlacePredictionAmountComponent(
    appComponentContext: AppComponentContext,
    private val params: PlacePredictionComponent.Params,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(space = 4.dp)) {
            Text(text = "Amount")
            Text(text = "${params.side} ${params.assetId}")
            Text(text = "market ${params.marketId}")
        }
    }
}