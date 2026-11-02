package com.tangem.features.polymarket.impl.placeprediction.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PredictionFeeSheetUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown
import com.tangem.features.polymarket.impl.placeprediction.entity.feeBreakdownQuote
import com.tangem.features.polymarket.impl.placeprediction.model.converter.PredictionFeeSheetUMConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

internal class PlacePredictionFeeSheet(
    private val uiState: StateFlow<PlacePredictionUM>,
    scope: CoroutineScope,
) {

    val navigation = SlotNavigation<Unit>()

    val state: StateFlow<PredictionFeeSheetUM?> = uiState
        .map(::createFeeSheet)
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = createFeeSheet(uiState.value))

    init {
        state
            .filter { it == null }
            .onEach { navigation.dismiss() }
            .launchIn(scope)
    }

    fun onFeeClick() {
        if (uiState.value.canShowFeeBreakdown()) navigation.activate(Unit)
    }

    private fun createFeeSheet(state: PlacePredictionUM): PredictionFeeSheetUM? {
        val content = state.feeBreakdownQuote() ?: return null

        return PredictionFeeSheetUMConverter.convert(
            quote = content.signable.quote,
            slippage = state.slippage,
            tokenSymbol = state.collateral.tokenSymbol,
        )
    }
}