package com.tangem.features.foryou.impl.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import com.tangem.features.foryou.impl.components.state.DonutSegmentUM
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Builds the [DonutSegmentTooltip] position provider anchored to the end of the selected slice. Returns the
 * centered fallback while the chart hasn't been measured yet or nothing is selected.
 *
 * @param startAngle Angle (degrees) where the first slice starts — must match the [DonutChart] drawing pass
 *   (`-90f` = 12 o'clock) so the anchor lands on the real slice end.
 */
@Suppress("MagicNumber", "LongParameterList", "ComplexCondition")
internal fun segmentTooltipPositionProvider(
    selectedIndex: Int?,
    segments: List<DonutSegmentUM>,
    chartSize: IntSize,
    chartWindowOffset: Offset,
    strokePx: Float,
    startAngle: Float,
    cardBoundsInWindow: Rect,
    gapPx: Int,
): PopupPositionProvider {
    if (selectedIndex == null || selectedIndex !in segments.indices ||
        chartSize.width == 0 || chartSize.height == 0
    ) {
        // Not shown in this state (selectedIndex is null / chart not measured) — position is irrelevant.
        return SegmentTooltipPositionProvider(Offset.Zero, Rect.Zero, gapPx)
    }
    val diameter = min(chartSize.width, chartSize.height).toFloat()
    val centerX = chartSize.width / 2f
    val centerY = chartSize.height / 2f
    val innerRadius = diameter / 2f - strokePx / 2
    // End angle of the selected slice (before its round cap) — same layout as DonutChart's drawing pass.
    val sweeps = segments.map { it.weight.toFloat().coerceIn(0f, 1f) * 360f }
    val endAngleDeg = startAngle + sweeps.take(selectedIndex + 1).sum()
    val endAngleRad = Math.toRadians(endAngleDeg.toDouble())
    val anchorLocal = Offset(
        x = centerX + innerRadius * cos(endAngleRad).toFloat(),
        y = centerY + innerRadius * sin(endAngleRad).toFloat() - strokePx / 2,
    )

    val anchorInWindow = chartWindowOffset + anchorLocal

    return SegmentTooltipPositionProvider(
        anchorInWindow = anchorInWindow,
        cardBoundsInWindow = cardBoundsInWindow,
        gapPx = gapPx,
        strokePx = strokePx.toInt(),
    )
}

/**
 * Positions the pill relative to the **end of the selected slice**, per the agreed spec.
 *
 * - **Base:** the pill's bottom-center sits [gapPx] above [anchorInWindow] (screen-up). [anchorInWindow] is
 *   the slice-end point on the ring's inner edge, in window coordinates.
 * - **Card fallback:** if that placement would push the pill above the top of [cardBoundsInWindow] (the
 *   `MarketChart` card), it flips to a side placement — the pill's start-center sits [gapPx] to the right
 *   of the anchor.
 * - **Screen clamp:** the result is finally kept inside the window with a [gapPx] margin (shifted back by
 *   however much it overflowed).
 *
 * @param anchorInWindow Slice-end / inner-edge point in window px.
 * @param cardBoundsInWindow `MarketChart` card bounds in window px (only the top edge gates the fallback).
 * @param gapPx The 8dp gap, in px.
 */
internal class SegmentTooltipPositionProvider(
    private val anchorInWindow: Offset,
    private val cardBoundsInWindow: Rect,
    private val gapPx: Int,
    private val strokePx: Int = 0,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val w = popupContentSize.width
        val h = popupContentSize.height
        val ax = anchorInWindow.x.roundToInt()
        val ay = anchorInWindow.y.roundToInt()

        var x = ax - w / 2
        var y = ay - h - gapPx

        val isFlip = y < cardBoundsInWindow.top

        if (isFlip) {
            x = ax + gapPx + strokePx / 2
            y = ay - h / 2 + strokePx / 2
        }

        // Keep the pill inside the card on every edge, shifting it back by however much it overflows
        // (with a gap margin). The card sits within the screen, so this also keeps the pill on-screen.
        val minX = cardBoundsInWindow.left.roundToInt() + gapPx
        val minY = cardBoundsInWindow.top.roundToInt() + gapPx
        val maxX = (cardBoundsInWindow.right.roundToInt() - w - gapPx).coerceAtLeast(minX)
        val maxY = (cardBoundsInWindow.bottom.roundToInt() - h - gapPx).coerceAtLeast(minY)
        val clampedX = x.coerceIn(minX, maxX)
        val clampedY = y.coerceIn(minY, maxY)

        return IntOffset(clampedX, clampedY)
    }
}