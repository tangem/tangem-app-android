package com.tangem.features.polymarket.impl.positiondetails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_16
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardShape
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Metrics
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Pnl

@Composable
internal fun PolymarketPositionMetrics(state: Metrics, onPnlClick: (Pnl) -> Unit, modifier: Modifier = Modifier) {
    val metrics = listOf(
        R.string.prediction_positions_quantity to state.quantity,
        R.string.prediction_positions_price_changes to state.priceChanges,
        R.string.prediction_positions_unrealized_pnl to state.unrealizedPnl,
        R.string.prediction_positions_realized_pnl to state.realizedPnl,
    )
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth / LocalDensity.current.fontScale < 300.dp) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            metrics.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    row.forEach { (title, value) ->
                        PositionMetric(
                            title = title,
                            value = value,
                            onPnlClick = onPnlClick,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionMetric(title: Int, value: TextReference, onPnlClick: (Pnl) -> Unit, modifier: Modifier = Modifier) {
    val pnl = when (title) {
        R.string.prediction_positions_unrealized_pnl -> Pnl.Unrealized
        R.string.prediction_positions_realized_pnl -> Pnl.Realized
        else -> null
    }
    TangemSurface(
        modifier = modifier.semantics { if (pnl != null) role = Role.Button },
        color = TangemTheme.colors3.bg.secondary,
        shape = PolymarketCardShape,
        onClick = pnl?.let { { onPnlClick(it) } },
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f, fill = false),
                    text = stringResourceSafe(title),
                    style = TangemTheme.typography3.caption.medium,
                    color = TangemTheme.colors3.text.secondary,
                )
                if (pnl != null) {
                    Icon(
                        imageVector = Icons.ic_info_16,
                        contentDescription = null,
                        tint = TangemTheme.colors3.icon.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}