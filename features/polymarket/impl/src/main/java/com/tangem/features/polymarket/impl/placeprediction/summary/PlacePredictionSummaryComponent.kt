package com.tangem.features.polymarket.impl.placeprediction.summary

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableContentComponent

internal class PlacePredictionSummaryComponent(
    appComponentContext: AppComponentContext,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    @Composable
    override fun Content(modifier: Modifier) {
        Box(modifier = modifier) {
            Text(text = "Summary")
        }
    }
}