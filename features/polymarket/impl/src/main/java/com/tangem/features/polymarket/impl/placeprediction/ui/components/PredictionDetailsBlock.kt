package com.tangem.features.polymarket.impl.placeprediction.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_double_vertical_16
import com.tangem.features.polymarket.impl.common.formatPolymarketMoney
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.placeprediction.entity.PlacePredictionUM
import com.tangem.features.polymarket.impl.placeprediction.entity.QuoteUM
import com.tangem.features.polymarket.impl.placeprediction.entity.canShowFeeBreakdown

@Composable
internal fun PredictionDetailsBlock(state: PlacePredictionUM, onFeeClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemSurface(modifier = modifier, color = TangemTheme.colors3.bg.secondary, shape = PolymarketCardShape) {
        Column {
            TangemRow(
                divider = true,
                verticalAlignment = TangemRowVerticalAlignment.Center,
                titleSlot = { DetailTitle(text = stringResourceSafe(R.string.prediction_place_fee)) },
                valueSlot = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TangemBadge(
                            text = stringReference(state.collateral.tokenSymbol),
                            size = TangemBadge.Size.X6,
                        )
                        DetailValue(
                            text = (state.quote as? QuoteUM.Content)?.feeTotal?.formatPolymarketMoney().orEmpty(),
                        )
                    }
                },
                endSlot = { DetailChevron() },
                onClick = onFeeClick.takeIf { state.canShowFeeBreakdown() },
            )
            PredictionSlippageRow(slippage = state.slippage)
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

@Composable
private fun DetailChevron() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_16,
        contentDescription = null,
        tint = TangemTheme.colors3.icon.tertiary,
    )
}