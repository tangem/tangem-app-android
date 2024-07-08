@file:Suppress("MagicNumber")
package com.tangem.features.markets.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.features.markets.ui.entity.MarketsListItemUM

internal class MarketChartListItemPreviewDataProvider : CollectionPreviewParameterProvider<MarketsListItemUM>(
    collection = listOf(
        MarketsListItemUM(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = "",
            ratingPosition = "10",
            marketCap = "$6.233 B",
            price = MarketsListItemUM.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemUM(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = "10",
            marketCap = "$6.233 B",
            price = MarketsListItemUM.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.NEUTRAL,
            chardData = null,
        ),
        MarketsListItemUM(
            id = "1",
            name = "Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = "10",
            marketCap = "$6.23348172384781234 B",
            price = MarketsListItemUM.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.DOWN,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemUM(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = "10",
            marketCap = null,
            price = MarketsListItemUM.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemUM(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = null,
            marketCap = "$6.233 B",
            price = MarketsListItemUM.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemUM(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = null,
            marketCap = null,
            price = MarketsListItemUM.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
    ),
)
