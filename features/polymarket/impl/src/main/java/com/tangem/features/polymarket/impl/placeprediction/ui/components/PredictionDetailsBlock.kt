package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown

@Composable
internal fun PredictionDetailsBlock(state: PlacePredictionUM, onFeeClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
        Column {
            PredictionUsdValueRow(
                titleRes = R.string.prediction_place_fee,
                value = (state.quote as? QuoteUM.Content)?.feeTotal?.formatPolymarketMoney().orEmpty(),
                isLoading = false,
                divider = true,
                onClick = onFeeClick.takeIf { state.canShowFeeBreakdown() },
            )
            PredictionSlippageRow(slippage = state.slippage)
        }
    }
}