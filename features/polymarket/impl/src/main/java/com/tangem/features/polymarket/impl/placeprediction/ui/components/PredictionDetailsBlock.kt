package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM

/**
 * Fee and slippage, both read-only here: the fee breakdown popup and the slippage sheet belong to the
 * amount step, so this screen states the figures the order will be placed with and nothing else.
 */
@Composable
internal fun PredictionDetailsBlock(state: PlacePredictionUM, modifier: Modifier = Modifier) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary) {
        Column {
            TangemRow(
                divider = true,
                titleSlot = { DetailTitle(text = stringResourceSafe(R.string.prediction_place_fee)) },
                valueSlot = {
                    DetailValue(
                        text = (state.quote as? QuoteUM.Content)?.feeTotal?.formatPolymarketMoney().orEmpty(),
                    )
                },
            )
            TangemRow(
                titleSlot = { DetailTitle(text = stringResourceSafe(R.string.prediction_place_slippage)) },
                valueSlot = { DetailValue(text = "${state.slippage.percent}%") },
            )
        }
    }
}

@Composable
private fun DetailTitle(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
    )
}

@Composable
private fun DetailValue(text: String) {
    Text(
        text = text,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.secondary,
    )
}