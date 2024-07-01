@file:Suppress("MagicNumber")
package com.tangem.features.markets.ui.preview

import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.features.markets.ui.models.MarketsListItemModel

internal class MarketChartListItemPreviewDataProvider : CollectionPreviewParameterProvider<MarketsListItemModel>(
    collection = listOf(
        MarketsListItemModel(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = "",
            ratingPosition = "10",
            marketCap = "$6.233 B",
            price = MarketsListItemModel.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemModel(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = "10",
            marketCap = "$6.233 B",
            price = MarketsListItemModel.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.NEUTRAL,
            chardData = null,
        ),
        MarketsListItemModel(
            id = "1",
            name = "Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = "10",
            marketCap = "$6.23348172384781234 B",
            price = MarketsListItemModel.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.DOWN,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemModel(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = "10",
            marketCap = null,
            price = MarketsListItemModel.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemModel(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = null,
            marketCap = "$6.233 B",
            price = MarketsListItemModel.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
        MarketsListItemModel(
            id = "1",
            name = "Bitcoin",
            currencySymbol = "BTC",
            iconUrl = null,
            ratingPosition = null,
            marketCap = null,
            price = MarketsListItemModel.Price(text = "31 285.72$"),
            trendPercentText = "12.43%",
            trendType = PriceChangeType.UP,
            chardData = MarketChartRawData(
                y = listOf(0.4f, 0.2f, 0.4f, 0.1f, 0.4f, 2f, 5f, 0.1f, 2f, 2f, 3f),
            ),
        ),
    ),
)