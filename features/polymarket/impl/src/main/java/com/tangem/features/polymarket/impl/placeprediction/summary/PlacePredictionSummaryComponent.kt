package com.tangem.features.polymarket.impl.placeprediction.summary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.features.polymarket.impl.placeprediction.entity.PreparedPrediction
import com.tangem.features.polymarket.impl.placeprediction.model.PlacePredictionSummaryModel
import com.tangem.features.polymarket.impl.placeprediction.ui.PlacePredictionSummaryContent

internal class PlacePredictionSummaryComponent(
    appComponentContext: AppComponentContext,
    params: PreparedPrediction,
) : ComposableContentComponent, AppComponentContext by appComponentContext {

    private val model: PlacePredictionSummaryModel = getOrCreateModel(params = params)

    private val feeSheetSlot = childSlot(
        source = model.feeSheetNavigation,
        serializer = null,
        key = "feeSheet",
        handleBackButton = false,
        childFactory = { _, componentContext ->
            PredictionFeeSheetComponent(appComponentContext = childByContext(componentContext), model = model)
        },
    )

    fun isSubmitting(): Boolean = model.isSubmitting()

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.uiState.collectAsStateWithLifecycle()
        val feeSheet by feeSheetSlot.subscribeAsState()

        PlacePredictionSummaryContent(state = state, intents = model, modifier = modifier)

        feeSheet.child?.instance?.BottomSheet()
    }
}