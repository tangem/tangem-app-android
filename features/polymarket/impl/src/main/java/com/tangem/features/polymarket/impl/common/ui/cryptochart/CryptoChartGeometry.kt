package com.tangem.features.polymarket.impl.common.ui.cryptochart

import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Point
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.TimeRange
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.max

internal data class CryptoChartScale(val min: Double, val max: Double) {
    fun y(price: Double): Float = ((max - price) / (max - min)).toFloat()

    fun ticks(count: Int): List<Double> = List(count) { max - (max - min) * it / (count - 1) }

    fun fractionDigits(count: Int): Int = ceil(-log10((max - min) / (count - 1))).toInt().coerceAtLeast(0)

    companion object {
        fun from(points: List<Point>, target: Double?): CryptoChartScale {
            val low = points.minOfOrNull { it.price } ?: target ?: 0.0
            val high = points.maxOfOrNull { it.price } ?: low
            val padding = max((high - low) * RANGE_PADDING, max(abs(high) * FLAT_PADDING, MIN_PADDING))
            return CryptoChartScale(min = (low - padding).coerceAtLeast(0.0), max = high + padding)
        }
    }
}

internal fun TimeRange.x(timestamp: Long): Float =
    ((timestamp - startMillis).toDouble() / (endMillis - startMillis)).toFloat()

internal fun List<Point>.priceAt(timestamp: Long): Point? {
    val index = binarySearchBy(timestamp) { it.timestampMillis }
    if (index >= 0) return this[index]
    val upper = -index - 1
    if (upper == 0 || upper == size || this[upper].isSegmentStart) return null
    val left = this[upper - 1]
    val right = this[upper]
    val fraction = (timestamp - left.timestampMillis).toDouble() / (right.timestampMillis - left.timestampMillis)
    return Point(timestamp, left.price + (right.price - left.price) * fraction)
}

/** Clips to actual observations and interpolates only inside continuous segments, never beyond the latest quote. */
internal fun List<Point>.visiblePoints(range: TimeRange): List<Point> {
    fun insertion(timestamp: Long): Int {
        val index = binarySearchBy(timestamp) { it.timestampMillis }
        return if (index >= 0) index else -index - 1
    }
    val start = insertion(range.startMillis)
    val end = insertion(range.endMillis)
    val leading = priceAt(range.startMillis).takeIf { getOrNull(start)?.timestampMillis != range.startMillis }
    val trailing = priceAt(range.endMillis)
    return buildList {
        leading?.let(::add)
        addAll(this@visiblePoints.subList(start, end))
        trailing?.let(::add)
    }
}

private const val RANGE_PADDING = 0.12
private const val FLAT_PADDING = 0.0001
private const val MIN_PADDING = 1e-12