@file:Suppress("MagicNumber")

package com.tangem.feature.tester.presentation.storybook.page.ds.tokenrowmarket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.Shimmer
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.presentation.storybook.entity.TangemTokenRowMarketStory
import kotlinx.collections.immutable.persistentListOf

private const val SAMPLE_ICON_URL = "https://s3.eu-central-1.amazonaws.com/tangem.api/coins/large/bitcoin.png"

@Composable
internal fun TangemTokenRowMarketStory(state: TangemTokenRowMarketStory, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary)
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ComponentPreview(state = state)
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DirectionSelector(selected = state.direction, onSelect = state.onDirectionChange)
            Toggles(state = state)
        }
    }
}

@Composable
private fun ComponentPreview(state: TangemTokenRowMarketStory) {
    val icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = SAMPLE_ICON_URL))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors3.bg.primary)
            .padding(vertical = 16.dp),
    ) {
        if (state.isShimmer) {
            TangemTokenRowMarket.Shimmer()
        } else {
            TangemTokenRowMarket(
                icon = icon,
                title = if (state.longTitle) {
                    stringReference("Very Long Token Name Coin")
                } else {
                    stringReference("Bitcoin")
                },
                ticker = if (state.hasTicker) stringReference("BTC") else null,
                position = if (state.hasPosition) stringReference("2") else null,
                capitalization = if (state.hasCapitalization) stringReference("1.196T") else null,
                price = if (state.hasPrice) stringReference("$59,723.24") else null,
                priceChange = if (state.hasPriceChange) {
                    TangemPriceChange.State(value = stringReference("2.08%"), direction = state.direction)
                } else {
                    null
                },
                chart = if (state.hasChart) {
                    { MarketChart(direction = state.direction) }
                } else {
                    null
                },
                onClick = {},
            )
        }
    }
}

// The graph slot is filled with the real MarketChartMini from :common:ui-charts, sized 24x32
// (collapsed) per the Figma spec, showing how a feature wires the chart into the DS slot.
@Composable
private fun MarketChart(direction: TangemPriceChange.Direction) {
    val type = when (direction) {
        TangemPriceChange.Direction.Up -> MarketChartLook.Type.Growing
        TangemPriceChange.Direction.Down -> MarketChartLook.Type.Falling
        TangemPriceChange.Direction.Neutral -> MarketChartLook.Type.Neutral
    }
    MarketChartMini(
        rawData = PreviewChartData,
        type = type,
        modifier = Modifier.size(width = 24.dp, height = 32.dp),
    )
}

private val PreviewChartData = MarketChartRawData(
    y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
)

@Composable
private fun DirectionSelector(selected: TangemPriceChange.Direction, onSelect: (TangemPriceChange.Direction) -> Unit) {
    Section(label = "Price change") {
        ChipGrid(
            items = TangemPriceChange.Direction.entries,
            label = { it.name },
            isSelected = { it == selected },
            onSelect = onSelect,
        )
    }
}

@Composable
private fun Toggles(state: TangemTokenRowMarketStory) {
    Section(label = "Flags") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleRow(label = "shimmer", checked = state.isShimmer, onToggle = state.onShimmerToggle)
            ToggleRow(label = "ticker", checked = state.hasTicker, onToggle = state.onTickerToggle)
            ToggleRow(label = "position", checked = state.hasPosition, onToggle = state.onPositionToggle)
            ToggleRow(
                label = "capitalization",
                checked = state.hasCapitalization,
                onToggle = state.onCapitalizationToggle,
            )
            ToggleRow(label = "price", checked = state.hasPrice, onToggle = state.onPriceToggle)
            ToggleRow(label = "priceChange", checked = state.hasPriceChange, onToggle = state.onPriceChangeToggle)
            ToggleRow(label = "chart", checked = state.hasChart, onToggle = state.onChartToggle)
            ToggleRow(label = "longTitle", checked = state.longTitle, onToggle = state.onLongTitleToggle)
        }
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = label,
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
        content()
    }
}

@Composable
private fun <T> ChipGrid(items: List<T>, label: (T) -> String, isSelected: (T) -> Boolean, onSelect: (T) -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(TangemTheme.colors2.surface.level2)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.secondary,
                shape = shape,
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items.forEach { item ->
            Chip(
                label = label(item),
                selected = isSelected(item),
                onClick = { onSelect(item) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val chipShape = RoundedCornerShape(50)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(chipShape)
            .background(
                if (selected) TangemTheme.colors2.surface.level3 else TangemTheme.colors2.surface.level2,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.caption2,
            color = if (selected) TangemTheme.colors.text.primary1 else TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(TangemTheme.colors2.surface.level2)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
        Text(
            text = if (checked) "ON" else "OFF",
            style = TangemTheme.typography.caption2,
            color = if (checked) TangemTheme.colors.text.accent else TangemTheme.colors.text.secondary,
        )
    }
}