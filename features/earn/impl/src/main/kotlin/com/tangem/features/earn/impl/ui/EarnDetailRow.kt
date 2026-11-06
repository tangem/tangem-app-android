package com.tangem.features.earn.impl.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_top_right_16
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.features.earn.api.ui.EarnRowUM

/**
 * A label and a value. The ⓘ stands right after the label when the row has an explanation to open; a link value
 * (`onValueClick`) has an arrow after it.
 */
@Composable
internal fun EarnDetailRow(state: EarnRowUM, modifier: Modifier = Modifier, divider: Boolean = false) {
    TangemRow(
        modifier = modifier,
        divider = divider,
        titleSlot = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.label.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                state.onInfoClick?.let { onInfoClick -> InfoButton(onClick = onInfoClick) }
            }
        },
        valueSlot = { Value(state = state) },
    )
}

@Composable
private fun Value(state: EarnRowUM) {
    val onValueClick = state.onValueClick
    if (onValueClick == null) {
        TangemRowText(text = state.value, role = TangemRowTextRole.Value)
    } else {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onValueClick),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TangemRowText(text = state.value, role = TangemRowTextRole.Value)
            Icon(
                imageVector = Icons.ic_arrow_top_right_16,
                contentDescription = null,
                tint = TangemTheme.colors3.icon.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun InfoButton(onClick: () -> Unit) {
    Icon(
        imageVector = Icons.ic_info_20,
        contentDescription = null,
        tint = TangemTheme.colors3.icon.tertiary,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .size(20.dp),
    )
}