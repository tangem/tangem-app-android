package com.tangem.data.yield.supply.converters

import com.tangem.datasource.api.tangemTech.models.YieldTokenChartResponse
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData

internal object YieldTokenChartConverter {

    fun convert(response: YieldTokenChartResponse): YieldSupplyMarketChartData {
        val y = response.data.map { it.avgApy.toDouble() }
        val x = response.data.map { it.bucketIndex.toDouble() }
        val avr = response.averageApy.toDouble()
        return YieldSupplyMarketChartData(y = y, x = x, avr = avr)
    }
}