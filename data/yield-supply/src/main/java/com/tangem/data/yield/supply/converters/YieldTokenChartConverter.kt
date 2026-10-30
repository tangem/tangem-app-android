package com.tangem.data.yield.supply.converters

import com.tangem.grow.datasource.yield.models.YieldTokenChartResponse
import com.tangem.domain.yield.supply.models.YieldSupplyMarketChartData
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal object YieldTokenChartConverter {

    fun convert(response: YieldTokenChartResponse): YieldSupplyMarketChartData {
        val y = response.data.map { it.avgApy.toDouble() }
        val x = response.data.map { it.bucketIndex.toDouble() }
        val avr = response.averageApy.toDouble()
        return YieldSupplyMarketChartData(y = y, x = x, avr = avr, bucketStarts = bucketStarts(response.data))
    }

    /** All or nothing: an unreadable date must not fail the chart. */
    private fun bucketStarts(points: List<YieldTokenChartResponse.DataPoint>): List<LocalDate> {
        val dates = points.mapNotNull { point -> point.start?.toUtcDateOrNull() }
        return if (dates.size == points.size) dates else emptyList()
    }

    private fun String.toUtcDateOrNull(): LocalDate? = runCatching {
        Instant.parse(this).toLocalDateTime(TimeZone.UTC).date
    }.getOrNull()
}