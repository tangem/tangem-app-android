package com.tangem.features.collectibles.impl.machinedetails.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.collectibles.impl.machinedetails.ui.state.CollectiblesMachineDetailsUM

/** A rarity tier: its name with the tier dot, and the value range with the drop chance below. */
@Composable
internal fun CollectiblesTierCard(state: CollectiblesMachineDetailsUM.TierUM, modifier: Modifier = Modifier) {
    CollectiblesStatCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.name.resolveReference(),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weight = 1f, fill = false),
            )
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(state.accent.toColor())
                    .border(width = 1.dp, color = TangemTheme.colors3.border.secondary, shape = CircleShape),
            )
        }
        Text(
            text = state.odds.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A pack fact: its label, and the value below. */
@Composable
internal fun CollectiblesDetailCard(state: CollectiblesMachineDetailsUM.DetailUM, modifier: Modifier = Modifier) {
    CollectiblesStatCard(modifier = modifier) {
        Text(
            text = state.label.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = state.value.resolveReference(),
            style = TangemTheme.typography3.body.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CollectiblesStatCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content,
    )
}