@file:Suppress("MagicNumber")
package com.tangem.features.markets.tokenlist.impl.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListItemUM
import kotlinx.collections.immutable.persistentListOf

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
                y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
            ),
            isUnder100kMarketCap = false,
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
            isUnder100kMarketCap = false,
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
                y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
            ),
            isUnder100kMarketCap = false,
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
                y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
            ),
            isUnder100kMarketCap = false,
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
                y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
            ),
            isUnder100kMarketCap = false,
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
                y = persistentListOf(0.4, 0.2, 0.4, 0.1, 0.4, 2.0, 5.0, 0.1, 2.0, 2.0, 3.0),
            ),
            isUnder100kMarketCap = false,
        ),
    ),
)
