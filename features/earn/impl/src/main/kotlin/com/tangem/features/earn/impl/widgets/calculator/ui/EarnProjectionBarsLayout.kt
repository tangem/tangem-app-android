package com.tangem.features.earn.impl.widgets.calculator.ui

import androidx.compose.ui.geometry.Offset
import kotlin.math.roundToInt

internal class EarnBarsGeometry(
    width: Float,
    private val barCount: Int,
    maxBarWidth: Float,
) {

    val barWidth: Float = if (barCount > 0) minOf(width / (barCount * 2 - 1), maxBarWidth) else 0f

    private val gap: Float = if (barCount > 1) (width - barWidth * barCount) / (barCount - 1) else 0f

    private val stride: Float = barWidth + gap

    fun barLeft(index: Int): Float = index * stride

    fun barCenter(index: Int): Float = barLeft(index) + barWidth / 2

    fun barAt(x: Float): Int? {
        if (barCount <= 0 || stride <= 0f) return null
        val nearest = ((x - barWidth / 2) / stride).roundToInt()
        return nearest.coerceIn(0, barCount - 1)
    }
}

@Suppress("LongParameterList")
internal fun earnTooltipPosition(
    barCenter: Float,
    barWidth: Float,
    barTop: Float,
    tooltipWidth: Float,
    tooltipHeight: Float,
    chartWidth: Float,
    spacing: Float,
): Offset {
    val aboveTop = barTop - tooltipHeight - spacing
    val hasRoomAbove = aboveTop >= 0f

    val y = if (hasRoomAbove) aboveTop else 0f
    val x = when {
        hasRoomAbove -> barCenter - tooltipWidth / 2
        barCenter > chartWidth / 2 -> barCenter - barWidth / 2 - spacing - tooltipWidth
        else -> barCenter + barWidth / 2 + spacing
    }

    return Offset(
        x = x.coerceAtMost(chartWidth - tooltipWidth).coerceAtLeast(0f),
        y = y,
    )
}