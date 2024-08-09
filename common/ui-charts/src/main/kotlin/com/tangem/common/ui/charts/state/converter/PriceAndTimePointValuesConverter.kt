package com.tangem.common.ui.charts.state.converter

import com.tangem.common.ui.charts.downsample.LTThreeBuckets
import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartRawData
import kotlinx.collections.immutable.toImmutableList
import java.math.BigDecimal

@Suppress("MagicNumber")
class PriceAndTimePointValuesConverter(
    private val needToFormatAxis: Boolean,
) : PointValuesConverter {

    private data class MinMaxCache(
        val minX: BigDecimal,
        val maxX: BigDecimal,
        val minY: BigDecimal,
        val maxY: BigDecimal,
    )

    private var minMaxCache = MinMaxCache(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
    private val formatYValuesCache = mutableMapOf<Double, BigDecimal>()
    private val formatXValuesCache = mutableMapOf<Double, BigDecimal>()

    override fun convert(data: MarketChartData.Data): MarketChartRawData {
        formatYValuesCache.clear()
        formatXValuesCache.clear()
        val cache = MinMaxCache(
            minY = data.y.minOrNull() ?: BigDecimal.ZERO,
            maxY = data.y.maxOrNull() ?: BigDecimal.ZERO,
            minX = data.x.minOrNull() ?: BigDecimal.ZERO,
            maxX = data.x.maxOrNull() ?: BigDecimal.ZERO,
        )
        minMaxCache = cache

        val normY = data.y.normalizeToDouble(min = cache.minY, max = cache.maxY)
        val normX = data.x.normalizeTime(min = cache.minX, max = cache.maxX)

        return if (normX.size > MAX_POINTS) {
            LTThreeBuckets
                .downsample(normX, normY, MAX_POINTS - 2)
                .let {
                    MarketChartRawData(
                        originalIndexes = it.originalIndexes.toImmutableList(),
                        x = it.x.toImmutableList(),
                        y = it.y.toImmutableList(),
                    )
                }
        } else {
            MarketChartRawData(
                x = normX.toImmutableList(),
                y = normY.toImmutableList(),
            )
        }
    }

    override fun prepareRawXForFormat(rawX: Double, data: MarketChartData.Data): BigDecimal {
        if (!needToFormatAxis) return BigDecimal.ZERO
        if (formatXValuesCache.containsKey(rawX)) return formatXValuesCache[rawX]!!

        val result = (rawX * MINUTE).toBigDecimal()

        formatXValuesCache[rawX] = result
        return result
    }

    override fun prepareRawYForFormat(rawY: Double, data: MarketChartData.Data): BigDecimal {
        if (!needToFormatAxis) return BigDecimal.ZERO
        if (formatYValuesCache.containsKey(rawY)) return formatYValuesCache[rawY]!!

        val min = minMaxCache.minY
        val max = minMaxCache.maxY
        val length = max - min

        val result = when {
            rawY < 0.01f -> min
            rawY < 0.55f && rawY > 0.45f -> min + length / 2.toBigDecimal()
            rawY > 0.97f && rawY < 1.01f -> max
            else -> length * rawY.toBigDecimal() + min
        }
        formatYValuesCache[rawY] = result
        return result
    }

    private fun List<BigDecimal>.normalizeToDouble(min: BigDecimal, max: BigDecimal): List<Double> {
        if (min == max) {
            return List(size) { 0.5 }
        }

        return map { ((it - min) / (max - min)).toDouble() }
    }

    private fun List<BigDecimal>.normalizeTime(min: BigDecimal, max: BigDecimal): List<Double> {
        if (min == max) {
            return List(size) { 0.5 }
        }

        return map {
            (it / MINUTE_BIG).toDouble()
        }
    }

    private companion object {
        private const val MAX_POINTS = 502
        private const val MINUTE = 60000L
        private val MINUTE_BIG = 60000L.toBigDecimal()
    }
}