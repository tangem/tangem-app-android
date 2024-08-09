package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketChartResponse
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenChart

class TokenChartConverter {

    fun convert(interval: PriceChangeInterval, value: TokenMarketChartResponse): TokenChart {
        return TokenChart(
            interval = interval,
            priceY = value.prices.values.toList(),
            timeStamps = value.prices.keys.toList(),
        )
    }
}
