package com.tangem.features.polymarket.impl.positiondetails.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.ds2.badge.TangemBadge
import com.tangem.core.ui.ds2.messagebubble.TangemMessageBubble
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_dot_24_filled
import com.tangem.core.ui.res.generated.icons.ic_triangle_down_28
import com.tangem.core.ui.res.generated.icons.ic_triangle_up_28
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardIcon
import com.tangem.features.polymarket.impl.positiondetails.ui.state.PolymarketPositionDetailsUM.Summary

@Composable
internal fun PolymarketPositionSummary(state: Summary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PolymarketCardIcon(iconUrl = state.iconUrl, modifier = Modifier.padding(bottom = 16.dp).size(72.dp))
        PositionPnl(state.pnl)
        state.currentValue?.let { currentValue ->
            Text(
                text = currentValue.resolveReference(),
                style = TangemTheme.typography3.heading.small,
                color = TangemTheme.colors3.text.primary,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TangemMessageBubble(
                modifier = Modifier.alignBy(FirstBaseline),
                text = state.outcomeLabel,
                variant = state.outcomeVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            state.payout?.let { payout ->
                TangemBadge(
                    modifier = Modifier.alignBy(FirstBaseline),
                    text = resourceReference(R.string.prediction_positions_payout, wrappedList(payout)),
                    status = TangemBadge.Status.Warning,
                    size = TangemBadge.Size.X6,
                )
            }
            state.result?.let { result ->
                TangemBadge(
                    modifier = Modifier.alignBy(FirstBaseline),
                    text = result.text,
                    variant = result.variant,
                    status = result.status,
                    size = result.size,
                    iconStart = result.iconStart,
                )
            }
        }
    }
}

@Composable
private fun PositionPnl(state: TangemPriceChange.State) {
    val color = when (state.direction) {
        TangemPriceChange.Direction.Up -> TangemTheme.colors3.text.accent.blue
        TangemPriceChange.Direction.Down -> TangemTheme.colors3.text.accent.red
        TangemPriceChange.Direction.Neutral -> TangemTheme.colors3.text.tertiary
    }
    val icon = when (state.direction) {
        TangemPriceChange.Direction.Up -> Icons.ic_triangle_up_28
        TangemPriceChange.Direction.Down -> Icons.ic_triangle_down_28
        TangemPriceChange.Direction.Neutral -> Icons.ic_dot_24_filled
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Text(
            text = state.value.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = color,
            textAlign = TextAlign.Center,
        )
    }
}