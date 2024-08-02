package com.tangem.features.markets.ui.entity

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.marketprice.PriceChangeType

@Immutable
data class MarketsListItemUM(
    val id: String,
    val name: String,
    val currencySymbol: String,
    val iconUrl: String?,
    val ratingPosition: String?,
    val marketCap: String?,
    val price: Price,
    val trendPercentText: String,
    val trendType: PriceChangeType,
    val chardData: MarketChartRawData?,
    val showUnder100kMarketCap: Boolean = false,
) {
    val chartType: MarketChartLook.Type = when (trendType) {
        PriceChangeType.UP,
        PriceChangeType.NEUTRAL,
        -> MarketChartLook.Type.Growing
        PriceChangeType.DOWN -> MarketChartLook.Type.Falling
    }

    @Immutable
    data class Price(
        val text: String,
        val changeType: PriceChangeType? = null,
    )
}