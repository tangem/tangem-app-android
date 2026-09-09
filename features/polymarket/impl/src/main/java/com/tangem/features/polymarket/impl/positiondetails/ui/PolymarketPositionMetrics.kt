package com.tangem.features.polymarket.impl.positiondetails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Metrics

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PolymarketPositionMetrics(state: Metrics, modifier: Modifier = Modifier) {
    val metrics = listOf(
        R.string.prediction_positions_quantity to state.quantity,
        R.string.prediction_positions_price_changes to state.priceChanges,
        R.string.prediction_positions_unrealized_pnl to state.unrealizedPnl,
        R.string.prediction_positions_realized_pnl to state.realizedPnl,
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = if (maxWidth / LocalDensity.current.fontScale < 300.dp) 1 else 2,
        ) {
            metrics.forEach { (title, value) ->
                TangemSurface(
                    modifier = Modifier.weight(1f).fillMaxRowHeight(),
                    color = TangemTheme.colors3.bg.secondary,
                    shape = PolymarketCardShape,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = value.resolveReference(),
                            style = TangemTheme.typography3.body.medium,
                            color = TangemTheme.colors3.text.primary,
                        )
                        Text(
                            text = stringResourceSafe(title),
                            style = TangemTheme.typography3.caption.medium,
                            color = TangemTheme.colors3.text.secondary,
                        )
                    }
                }
            }
        }
    }
}