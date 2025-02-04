package com.tangem.features.markets.tokenlist.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.marketprice.PriceChangeType
import com.tangem.domain.tokens.model.CryptoCurrency

@Immutable
data class MarketsListItemUM(
    val id: CryptoCurrency.RawID,
    val name: String,
    val currencySymbol: String,
    val iconUrl: String?,
    val ratingPosition: String?,
    val marketCap: String?,
    val price: Price,
    val trendPercentText: String,
    val trendType: PriceChangeType,
    val chardData: MarketChartRawData?,
    val isUnder100kMarketCap: Boolean,
) {
    val chartType: MarketChartLook.Type = when (trendType) {
        PriceChangeType.UP -> MarketChartLook.Type.Growing
        PriceChangeType.DOWN -> MarketChartLook.Type.Falling
        PriceChangeType.NEUTRAL -> MarketChartLook.Type.Neutral
    }

    @Immutable
    data class Price(
        val text: String,
        val changeType: PriceChangeType? = null,
    )
}