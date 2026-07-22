package com.tangem.features.polymarket.impl.placeprediction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

/**
 * Placeholder Place-prediction bottom sheet. Real UI (amount input, outcome selector) arrives in a later task.
 */
internal class PlacePredictionComponent(
    appComponentContext: AppComponentContext,
    private val params: Params,
    private val onDismiss: () -> Unit,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        onDismiss()
    }

    @Composable
    override fun BottomSheet() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "Place prediction")
            Text(text = "Event: ${params.eventId}")
        }
    }

    /**
     * @property eventId event the prediction is placed for
     * @property marketId optional preselected market inside the event
     * @property side optional preselected outcome side (asset id)
     */
    data class Params(
        val eventId: String,
        val marketId: String? = null,
        val side: String? = null,
    )
}