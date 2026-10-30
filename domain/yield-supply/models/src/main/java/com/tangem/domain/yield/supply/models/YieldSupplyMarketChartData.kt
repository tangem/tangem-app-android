package com.tangem.domain.yield.supply.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

/** @property bucketStarts first day of every bucket, parallel to [y]; empty when the backend did not date them */
@Serializable
data class YieldSupplyMarketChartData(
    val y: List<Double>,
    val x: List<Double>,
    val avr: Double,
    val bucketStarts: List<LocalDate> = emptyList(),
)