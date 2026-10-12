package com.tangem.features.polymarket.impl.placeprediction.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSummaryModel
import com.tangem.features.polymarket.impl.placeprediction.ui.PredictionFeeSheet

internal class PredictionFeeSheetComponent(
    appComponentContext: AppComponentContext,
    private val model: PlacePredictionSummaryModel,
) : ComposableBottomSheetComponent, AppComponentContext by appComponentContext {

    override fun dismiss() {
        model.feeSheetNavigation.dismiss()
    }

    @Composable
    override fun BottomSheet() {
        val state by model.feeSheetState.collectAsStateWithLifecycle()

        state?.let { PredictionFeeSheet(state = it, onDismiss = ::dismiss) }
    }
}