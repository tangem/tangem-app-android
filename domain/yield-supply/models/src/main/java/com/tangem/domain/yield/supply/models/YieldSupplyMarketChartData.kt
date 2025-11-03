package com.tangem.domain.yield.supply.models

import kotlinx.serialization.Serializable

@Serializable
data class YieldSupplyMarketChartData(
    val y: List<Double>,
    val x: List<Double>,
    val avr: Double,
)