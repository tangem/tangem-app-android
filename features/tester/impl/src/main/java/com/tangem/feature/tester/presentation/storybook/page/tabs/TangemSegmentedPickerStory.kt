@file:Suppress("MagicNumber", "LongMethod")
package com.tangem.feature.tester.presentation.storybook.page.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPicker
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val items2 = persistentListOf(
    TangemSegmentUM(id = "all", title = stringReference("All")),
    TangemSegmentUM(id = "tokens", title = stringReference("Tokens")),
)

private val items3 = persistentListOf(
    TangemSegmentUM(id = "1d", title = stringReference("1D")),
    TangemSegmentUM(id = "1w", title = stringReference("1W")),
    TangemSegmentUM(id = "1m", title = stringReference("1M")),
)

private val items4 = persistentListOf(
    TangemSegmentUM(id = "1d", title = stringReference("1D")),
    TangemSegmentUM(id = "1w", title = stringReference("1W")),
    TangemSegmentUM(id = "1m", title = stringReference("1M")),
    TangemSegmentUM(id = "1y", title = stringReference("1Y")),
)

private val items5 = persistentListOf(
    TangemSegmentUM(id = "send", title = stringReference("Send")),
    TangemSegmentUM(id = "receive", title = stringReference("Receive")),
    TangemSegmentUM(id = "swap", title = stringReference("Swap")),
    TangemSegmentUM(id = "buy", title = stringReference("Buy")),
    TangemSegmentUM(id = "sell", title = stringReference("Sell")),
)

private data class PickerConfig(
    val label: String,
    val isFixed: Boolean,
)

private val configs = listOf(
    PickerConfig("Default", isFixed = false),
    PickerConfig("Fixed", isFixed = true),
)

@Composable
internal fun TangemSegmentedPickerStory(modifier: Modifier = Modifier) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors2.surface.level1),
    ) {
        item("default_surface") {
            PickerSection(
                title = "Default surface",
                isAltSurface = false,
                background = TangemTheme.colors2.surface.level1,
            )
        }
        item("alt_surface") {
            PickerSection(
                title = "Alt surface",
                isAltSurface = true,
                background = TangemTheme.colors2.surface.level2,
            )
        }
        item("segment_count") {
            SegmentCountSection()
        }
    }
}

@Composable
private fun PickerSection(title: String, isAltSurface: Boolean, background: Color) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = title,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        configs.forEach { config ->
            PickerRow(config = config, isAltSurface = isAltSurface)
        }
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun PickerRow(config: PickerConfig, isAltSurface: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = config.label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        TangemSegmentedPicker(
            items = items4,
            isFixed = config.isFixed,
            isAltSurface = isAltSurface,
            onClick = {},
            modifier = if (config.isFixed) Modifier.fillMaxWidth() else Modifier,
        )
    }
}

@Composable
private fun SegmentCountSection() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(TangemTheme.colors2.surface.level1)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Segment count",
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        SegmentCountRow(label = "2 segments", items = items2)
        SegmentCountRow(label = "3 segments", items = items3)
        SegmentCountRow(label = "4 segments", items = items4)
        SegmentCountRow(label = "5 segments", items = items5)
    }
    HorizontalDivider(
        color = TangemTheme.colors2.border.neutral.secondary,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SegmentCountRow(label: String, items: ImmutableList<TangemSegmentUM>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.secondary,
        )
        TangemSegmentedPicker(
            items = items,
            isFixed = true,
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}