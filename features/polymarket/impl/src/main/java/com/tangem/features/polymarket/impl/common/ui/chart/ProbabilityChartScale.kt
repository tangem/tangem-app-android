package com.tangem.features.polymarket.impl.common.ui.chart

import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Point
import com.tangem.features.polymarket.impl.common.ui.chart.PolymarketProbabilityChart.Series
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sqrt

internal data class ProbabilityChartScale(
    val start: Long,
    val end: Long,
    val min: Double,
    val max: Double,
    val ticks: List<Double> = probabilityTicks(min, max),
) {

    fun x(timestamp: Long): Float = ((timestamp - start).toDouble() / (end - start)).toFloat()

    fun y(probability: Double): Float = ((max - probability) / (max - min)).toFloat()

    companion object {
        @Suppress("MagicNumber")
        val Placeholder =
            ProbabilityChartScale(start = 0, end = 1, min = 0.2, max = 0.65, ticks = listOf(0.65, 0.5, 0.35, 0.2))

        fun from(series: List<Series>): ProbabilityChartScale? {
            val points = series.flatMap { it.points }
            val start = points.minOfOrNull { it.timestampMillis } ?: return null
            val end = points.maxOf { it.timestampMillis }
            if (start == end) return null
            val low = points.minOf { it.probability }
            val high = points.maxOf { it.probability }
            val padding = if (low == high) FLAT_PADDING else (high - low) * PADDING_RATIO
            val span = (high - low + padding * 2).coerceAtLeast(MIN_SPAN).coerceAtMost(1.0)
            val min = ((low + high - span) / 2).coerceIn(0.0, 1.0 - span)
            return ProbabilityChartScale(start = start, end = end, min = min, max = min + span)
        }
    }
}

private fun probabilityTicks(min: Double, max: Double): List<Double> {
    val step = tickStep(max - min)
    return (ceil(min / step).toInt()..floor(max / step).toInt()).map { it * step }.reversed()
}

/** Never extrapolates a missing part of a series into a fabricated probability. */
internal fun List<Point>.probabilityAt(timestamp: Long): Double? {
    if (isEmpty() || timestamp < first().timestampMillis || timestamp > last().timestampMillis) return null
    val index = binarySearchBy(timestamp) { it.timestampMillis }
    if (index >= 0) return this[index].probability
    val upper = this[-index - 1]
    val lower = this[-index - 2]
    val fraction = (timestamp - lower.timestampMillis).toDouble() / (upper.timestampMillis - lower.timestampMillis)
    return lower.probability + (upper.probability - lower.probability) * fraction
}

@Suppress("MagicNumber")
private fun tickStep(span: Double): Double {
    // Same 1/2/5 tick spacing and bounded, padded scale as the iOS probability chart.
    val rough = span / TARGET_TICK_COUNT
    val magnitude = 10.0.pow(floor(log10(rough)))
    val normalized = rough / magnitude
    val factor = when {
        normalized >= sqrt(50.0) -> 10
        normalized >= sqrt(10.0) -> 5
        normalized >= sqrt(2.0) -> 2
        else -> 1
    }
    return (factor * magnitude).coerceAtLeast(MIN_TICK_STEP)
}

private const val PADDING_RATIO = 0.1
private const val FLAT_PADDING = 0.02
private const val MIN_SPAN = 0.03
private const val MIN_TICK_STEP = 0.01
private const val TARGET_TICK_COUNT = 4