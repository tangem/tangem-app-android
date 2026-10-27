package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.runtime.Composable
import com.tangem.features.polymarket.impl.placeprediction.entity.OrderSideUM
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import java.math.BigDecimal

@Composable
internal fun PredictionOrderAmountInput(
    state: PlacePredictionUM,
    onAmountChange: (String) -> Unit,
    onAddFundsClick: () -> Unit,
    isEnabled: Boolean = true,
) {
    when (val order = state.order) {
        is OrderSideUM.Buy -> PredictionAmountBlock(
            amountValue = state.amountValue,
            isAmountFromFraction = state.isAmountFromFraction,
            amountCaption = state.amountCaption,
            balance = order.balance,
            isAddFundsEnabled = order.isAddFundsEnabled,
            isEnabled = isEnabled,
            onAmountChange = onAmountChange,
            onAddFundsClick = onAddFundsClick,
        )
        is OrderSideUM.Sell -> PredictionSharesBlock(
            selected = state.amountValue.toBigDecimalOrNull() ?: BigDecimal.ZERO,
            position = order.position,
            proceeds = order.proceeds,
            isEnabled = isEnabled,
            onAmountChange = onAmountChange,
        )
    }
}