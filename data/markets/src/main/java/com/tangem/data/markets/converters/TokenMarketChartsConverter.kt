package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketChartListResponse
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.models.currency.CryptoCurrency

internal object TokenMarketChartsConverter {

    fun convert(
        chartsToCopy: TokenMarket.Charts,
        tokenId: CryptoCurrency.RawID,
        interval: TokenMarketListConfig.Interval,
        value: TokenMarketChartListResponse,
    ): TokenMarket.Charts {
        val prices = requireNotNull(value[tokenId.value]) {
            "$tokenId is not found in the response. This shouldn't have happened."
        }
        return when (interval) {
            TokenMarketListConfig.Interval.H24 -> chartsToCopy.copy(
                h24 = TokenChartConverter.convert(interval.toPriceChangeInterval(), prices),
            )
            TokenMarketListConfig.Interval.WEEK -> chartsToCopy.copy(
                week = TokenChartConverter.convert(interval.toPriceChangeInterval(), prices),
            )
            TokenMarketListConfig.Interval.MONTH -> chartsToCopy.copy(
                month = TokenChartConverter.convert(interval.toPriceChangeInterval(), prices),
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