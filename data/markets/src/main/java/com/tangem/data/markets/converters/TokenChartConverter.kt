package com.tangem.data.markets.converters

import com.tangem.datasource.api.markets.models.response.TokenMarketChartResponse
import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenChart

internal object TokenChartConverter {

    fun convert(
        interval: PriceChangeInterval,
        value: TokenMarketChartResponse,
        onNullPresented: () -> Unit = {},
    ): TokenChart {
        val points = value.prices.mapNotNull { p -> p.value?.let { p.key to it } }.toMap()

        if (points.size < points.values.size) {
            onNullPresented()
        }

        return TokenChart(
            interval = interval,
            priceY = points.values.toList(),
            timeStamps = points.keys.toList(),
        )
    }
}