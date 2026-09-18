package com.tangem.features.polymarket.impl.positions.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.ds.contextmenu.TangemContextMenu
import com.tangem.core.ui.ds2.filter.TangemFilterItem
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.domain.polymarket.model.PolymarketPositionsFilter

@Composable
internal fun PolymarketPositionsFilterDropdown(
    selected: PolymarketPositionsFilter,
    onSelect: (PolymarketPositionsFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }
    var anchorHeightPx by remember { mutableIntStateOf(0) }
    val menuOffset = with(LocalDensity.current) { DpOffset(x = 0.dp, y = -anchorHeightPx.toDp()) }
    Box(modifier = modifier.onSizeChanged { anchorHeightPx = it.height }) {
        TangemFilterItem(
            state = if (selected == PolymarketPositionsFilter.ALL) {
                TangemFilterItemUM.Inactive(
                    id = "positions_status",
                    label = selected.label,
                    onClick = { isExpanded = true },
                )
            } else {
                TangemFilterItemUM.Active(
                    id = "positions_status",
                    value = selected.label,
                    onClick = { isExpanded = true },
                    onClearClick = {
                        isExpanded = false
                        onSelect(PolymarketPositionsFilter.ALL)
                    },
                )
            },
            variant = TangemFilterItem.Variant.Transparent,
            clearContentDescription = stringResourceSafe(R.string.common_reset),
        )
        TangemContextMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.hazeEffectTangem { blurRadius = 10.dp },
            offset = menuOffset,
        ) {
            PositionsFilterOptions(selected = selected, onSelect = { filter ->
                isExpanded = false
                onSelect(filter)
            })
        }
    }
}

@Composable
private fun PositionsFilterOptions(selected: PolymarketPositionsFilter, onSelect: (PolymarketPositionsFilter) -> Unit) {
    Column(modifier = Modifier.selectableGroup().padding(vertical = 8.dp)) {
        PolymarketPositionsFilter.entries.forEach { filter ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(min = 200.dp)
                    .selectable(
                        selected = selected == filter,
                        role = Role.RadioButton,
                        onClick = { onSelect(filter) },
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                    if (selected == filter) {
                        Icon(
                            painter = painterResource(com.tangem.core.ui.R.drawable.ic_check_default_24),
                            contentDescription = null,
                            tint = TangemTheme.colors3.icon.primary,
                        )
                    }
                }
                Text(
                    text = filter.label.resolveReference(),
                    style = TangemTheme.typography3.body.medium,
                    color = TangemTheme.colors3.text.primary,
                )
            }
        }
    }
}

private val PolymarketPositionsFilter.label: TextReference
    get() = resourceReference(
        when (this) {
            PolymarketPositionsFilter.ALL -> R.string.common_all
            PolymarketPositionsFilter.ACTIVE -> R.string.prediction_positions_active
            PolymarketPositionsFilter.WON -> R.string.prediction_positions_won
            PolymarketPositionsFilter.LOST -> R.string.prediction_positions_lost
            PolymarketPositionsFilter.DISPUTED -> R.string.prediction_positions_disputed
            PolymarketPositionsFilter.PENDING -> R.string.prediction_positions_pending
            PolymarketPositionsFilter.SPLIT -> R.string.prediction_positions_split
        },
    )

@Preview(name = "Filter menu", widthDp = 240)
@Preview(name = "Filter menu dark", widthDp = 240, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Filter menu large text", widthDp = 240, fontScale = 1.5f)
@Composable
private fun PositionsFilterOptionsPreview() {
    var selected by remember { mutableStateOf(PolymarketPositionsFilter.ALL) }
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            PositionsFilterOptions(selected = selected, onSelect = { selected = it })
        }
    }
}