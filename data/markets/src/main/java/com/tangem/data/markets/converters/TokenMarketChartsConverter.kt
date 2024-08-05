package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketChartListResponse
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig

class TokenMarketChartsConverter(
    private val tokenChartConverter: TokenChartConverter,
) {

    fun convert(
        chartsToCopy: TokenMarket.Charts,
        tokenId: String,
        interval: TokenMarketListConfig.Interval,
        value: TokenMarketChartListResponse,
    ): TokenMarket.Charts {
        val prices = requireNotNull(value[tokenId]) {
            "$tokenId is not found in the response. This shouldn't have happened."
        }
        return when (interval) {
            TokenMarketListConfig.Interval.H24 -> chartsToCopy.copy(
                h24 = tokenChartConverter.convert(interval.toPriceChangeInterval(), prices),
            )
            TokenMarketListConfig.Interval.WEEK -> chartsToCopy.copy(
                week = tokenChartConverter.convert(interval.toPriceChangeInterval(), prices),
            )
            TokenMarketListConfig.Interval.MONTH -> chartsToCopy.copy(
                month = tokenChartConverter.convert(interval.toPriceChangeInterval(), prices),
            )
            else -> error("unsupported interval=$interval. This shouldn't have happened.")
        }
    }

    private fun TokenMarketListConfig.Interval.toPriceChangeInterval(): PriceChangeInterval = when (this) {
        TokenMarketListConfig.Interval.H24 -> PriceChangeInterval.H24
        TokenMarketListConfig.Interval.WEEK -> PriceChangeInterval.WEEK
        TokenMarketListConfig.Interval.MONTH -> PriceChangeInterval.MONTH
    }
}
