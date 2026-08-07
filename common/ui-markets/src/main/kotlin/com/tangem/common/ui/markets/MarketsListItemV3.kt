@file:Suppress("MagicNumber")

package com.tangem.common.ui.markets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.markets.preview.MarketChartListItemPreviewDataProvider
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalWindowSize
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.windowsize.WindowSizeType

/**
 * DS3 markets-list item — [TangemTokenRowMarket] bound to the legacy [MarketsListItemUM], for a
 * drop-in migration from [MarketsListItemV2].
 *
 * Behavior notes (kept from [MarketsListItemV2]):
 * - The chart is hidden on small window widths.
 * - While [MarketsListItemUM.chartData] is `null`, the graph slot shows a shimmer bar.
 * - On a live price update the price text flashes in the update-direction color
 *   ([MarketsListItemUM.Price.changeType]), like the legacy `TokenPriceText`.
 *
 * @param model Legacy markets list item model.
 * @param modifier Modifier applied to the row container.
 * @param onClick Row click handler. `null` makes the row non-interactive.
 */
@Composable
fun MarketsListItemV3(model: MarketsListItemUM, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val isChartVisible = LocalWindowSize.current.widthAtLeast(WindowSizeType.Small)
    TangemTokenRowMarket(
        icon = TangemTokenIcon.UiState.Token(TangemTokenIcon.State(url = model.iconUrl)),
        title = stringReference(model.name),
        modifier = modifier,
        ticker = stringReference(model.currencySymbol),
        position = model.ratingPosition?.let(::stringReference),
        capitalization = model.marketCap?.let(::stringReference),
        price = stringReference(model.price.text),
        priceChange = TangemPriceChange.State(
            value = stringReference(model.trendPercentText),
            direction = model.trendType.toPriceChangeDirection(),
        ),
        priceUpdateDirection = model.price.changeType?.toPriceChangeDirection(),
        chart = if (isChartVisible) {
            { MarketRowChart(chartType = model.chartType, chartRawData = model.chartData) }
        } else {
            null
        },
        onClick = onClick,
    )
}

private fun PriceChangeType.toPriceChangeDirection(): TangemPriceChange.Direction = when (this) {
    PriceChangeType.UP -> TangemPriceChange.Direction.Up
    PriceChangeType.DOWN -> TangemPriceChange.Direction.Down
    PriceChangeType.NEUTRAL -> TangemPriceChange.Direction.Neutral
}

/** Mini-graph for the [TangemTokenRowMarket] chart slot — 24x32 collapsed, shimmer while loading. */
@Composable
private fun MarketRowChart(chartType: MarketChartLook.Type, chartRawData: MarketChartRawData?) {
    Box(
        modifier = Modifier.size(width = 24.dp, height = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (chartRawData != null) {
            MarketChartMini(
                rawData = chartRawData,
                type = chartType,
                neutralColor = TangemTheme.colors3.border.accent.neutral,
                growingColor = TangemTheme.colors3.border.accent.blue,
                fallingColor = TangemTheme.colors3.border.accent.red,
            )
        } else {
            TangemShimmer(
                radius = 4.dp,
                modifier = Modifier.size(width = 24.dp, height = 16.dp),
            )
        }
    }
}

// region Preview

@Preview(name = "Light", showBackground = true, widthDp = 360)
@Preview(
    name = "Dark",
    showBackground = true,
    widthDp = 360,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun MarketsListItemV3Preview(
    @PreviewParameter(MarketChartListItemPreviewDataProvider::class) state: MarketsListItemUM,
) {
    TangemThemePreviewRedesign {
        Column(modifier = Modifier.background(TangemTheme.colors3.bg.primary)) {
            MarketsListItemV3(model = state, onClick = {})
            MarketsListItemV3(model = state.copy(chartData = null))
        }
    }
}

// endregion