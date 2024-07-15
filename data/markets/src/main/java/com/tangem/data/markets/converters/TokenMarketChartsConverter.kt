package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketChartListResponse
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarket

class TokenMarketChartsConverter(
    private val tokenListChartConverter: TokenListChartConverter,
) {

    fun convert(
        chartsToCopy: TokenMarket.Charts,
        tokenId: String,
        interval: PriceChangeInterval,
        value: TokenMarketChartListResponse,
    ): TokenMarket.Charts {
        val prices = requireNotNull(value[tokenId]) {
            "$tokenId is not found in the response. This shouldn't have happened."
        }
        return when (interval) {
            PriceChangeInterval.H24 -> chartsToCopy.copy(
                h24 = tokenListChartConverter.convert(interval, prices),
            )
            PriceChangeInterval.WEEK -> chartsToCopy.copy(
                week = tokenListChartConverter.convert(interval, prices),
            )
            PriceChangeInterval.MONTH -> chartsToCopy.copy(
                month = tokenListChartConverter.convert(interval, prices),
            )
            else -> error("unsupported interval=$interval. This shouldn't have happened.")
        }
    }
}
