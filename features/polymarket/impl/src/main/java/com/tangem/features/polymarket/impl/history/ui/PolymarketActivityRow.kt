package com.tangem.features.polymarket.impl.history.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.features.polymarket.impl.common.ui.PolymarketCardIcon
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketActivityRowUM.Type

@Composable
internal fun PolymarketActivityRow(state: PolymarketActivityRowUM, modifier: Modifier = Modifier) {
    TangemRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = TangemRowVerticalAlignment.Center,
        onClick = state.onClick,
        startSlot = { ActivityIcon(state = state) },
        titleSlot = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TangemRow(
                    includeInnerPaddings = false,
                    contentLead = TangemRowContentLead.Start,
                    titleSlot = { ActivityLabel(text = state.title, type = state.type) },
                    valueSlot = { ActivityLabel(text = state.amount, type = state.type) },
                )
                PolymarketActivitySubtitle(subtitle = state.subtitle)
            }
        },
    )
}

@Composable
private fun ActivityLabel(text: TextReference, type: Type) {
    Text(
        text = text.resolveReference(),
        color = if (type == Type.REDEEMED) {
            TangemTheme.colors3.text.status.success
        } else {
            TangemTheme.colors3.text.primary
        },
        style = TangemTheme.typography3.body.medium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ActivityIcon(state: PolymarketActivityRowUM) {
    when (state.type) {
        Type.FUNDS_ADDED, Type.WITHDRAWN -> Box(
            modifier = Modifier
                .size(40.dp)
                .background(TangemTheme.colors3.bg.opaque.secondary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (state.type == Type.FUNDS_ADDED) Icons.ic_arrow_down_24 else Icons.ic_arrow_up_24,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
                modifier = Modifier.size(24.dp),
            )
        }
        Type.BOUGHT, Type.SOLD, Type.REDEEMED, Type.LOST -> PolymarketCardIcon(iconUrl = state.iconUrl)
    }
}