package com.tangem.features.polymarket.impl.common.ui.cryptochart

import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Point
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.TimeRange
import kotlin.math.exp

internal data class CryptoChartFrame(val points: List<Point>, val range: TimeRange, val scale: CryptoChartScale) {
    fun pointAt(fraction: Float): Point? = points.priceAt(
        range.startMillis + ((range.endMillis - range.startMillis) * fraction.coerceIn(0f, 1f)).toLong(),
    )

    companion object {
        fun from(points: List<Point>, range: TimeRange, target: Double?): CryptoChartFrame {
            val visible = points.visiblePoints(range)
            return CryptoChartFrame(visible, range, CryptoChartScale.from(visible, target))
        }
    }
}

/** Display-only interpolation. The resulting prices must never be used for orders or settlement. */
internal class CryptoChartAnimator {
    private var previousInput: List<Point>? = null
    private var transitionFrom: CryptoChartFrame? = null
    private var lastFrame: CryptoChartFrame? = null
    private var transitionStartedAt = 0L
    private var previousFrameAt = 0L

    fun frame(
        points: List<Point>,
        range: TimeRange,
        target: Double?,
        frameMillis: Long,
        motionScale: Float,
    ): CryptoChartFrame {
        if (previousInput !== points) {
            previousInput?.let { previous ->
                advance(
                    points = previous,
                    range = range,
                    target = target,
                    frameMillis = frameMillis,
                    motionScale = motionScale,
                )
            }
            transitionFrom = lastFrame
            transitionStartedAt = frameMillis
            previousInput = points
        }
        return advance(
            points = points,
            range = range,
            target = target,
            frameMillis = frameMillis,
            motionScale = motionScale,
        )
    }

    private fun advance(
        points: List<Point>,
        range: TimeRange,
        target: Double?,
        frameMillis: Long,
        motionScale: Float,
    ): CryptoChartFrame {
        val raw = CryptoChartFrame.from(points, range, target)
        val old = lastFrame?.takeIf { it.points.isNotEmpty() }
        val displayed = if (old == null || motionScale == 0f || raw.points.isEmpty()) {
            raw
        } else {
            val fraction = ((frameMillis - transitionStartedAt) / (TRANSITION_MILLIS * motionScale)).coerceIn(0f, 1f)
            val visible = interpolate(points, raw, fraction)
            val desired = CryptoChartScale.from(visible, target)
            val weight = 1 - exp(-(frameMillis - previousFrameAt).toDouble() / (RANGE_SMOOTHING_MILLIS * motionScale))
            raw.copy(
                points = visible,
                scale = CryptoChartScale(
                    min = (old.scale.min + (desired.min - old.scale.min) * weight).coerceAtMost(
                        visible.minOf { it.price },
                    ),
                    max = (old.scale.max + (desired.max - old.scale.max) * weight).coerceAtLeast(
                        visible.maxOf { it.price },
                    ),
                ),
            )
        }
        previousFrameAt = frameMillis
        lastFrame = displayed
        return displayed
    }

    private fun interpolate(points: List<Point>, raw: CryptoChartFrame, fraction: Float): List<Point> {
        val source = transitionFrom ?: return raw.points
        if (fraction >= 1f) return raw.points
        val oldTip = source.points.lastOrNull() ?: return raw.points
        val newTip = raw.points.lastOrNull() ?: return raw.points
        val hasGap = raw.points.any { it.isSegmentStart && it.timestampMillis > oldTip.timestampMillis }
        val isContinuousHead = !hasGap && points.priceAt(oldTip.timestampMillis) != null
        val visible = if (isContinuousHead && newTip.timestampMillis > oldTip.timestampMillis &&
            oldTip.timestampMillis >= raw.range.startMillis
        ) {
            val elapsed = ((newTip.timestampMillis - oldTip.timestampMillis) * fraction).toLong()
            val tipTime = oldTip.timestampMillis + elapsed
            if (tipTime == raw.range.startMillis) {
                listOfNotNull(points.priceAt(tipTime))
            } else {
                points.visiblePoints(raw.range.copy(endMillis = tipTime))
            }
        } else {
            raw.points
        }
        return visible.map { point ->
            val before = source.points.priceAt(point.timestampMillis)
            if (before == null || hasGap) {
                point
            } else {
                point.copy(
                    price = before.price + (point.price - before.price) * fraction,
                )
            }
        }
    }
}

internal fun cryptoRenderTime(nowMillis: Long, delayMillis: Long, freezeAtMillis: Long?): Long =
    (nowMillis - delayMillis).coerceAtMost(freezeAtMillis ?: Long.MAX_VALUE)

private const val TRANSITION_MILLIS = 250f
private const val RANGE_SMOOTHING_MILLIS = 120.0