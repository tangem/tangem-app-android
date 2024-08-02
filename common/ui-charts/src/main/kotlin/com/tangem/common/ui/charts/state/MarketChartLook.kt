package com.tangem.common.ui.charts.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.formatter.AxisLabelFormatter

/**
 * This class represents the look and feel of a Market Chart.
 * It includes properties for type, marker highlight, animation on data change, animate data appearance,
 * and formatters for x and y axis.
 *
 * @property type The type of the chart, can be either Growing or Falling.
 * @property markerHighlightRightSide A boolean indicating whether the marker highlights the right side of the chart.
 * @property xAxisFormatter A formatter for the x-axis labels.
 * @property yAxisFormatter A formatter for the y-axis labels.
 */
@Immutable
data class MarketChartLook(
    val type: Type = Type.Growing,
    val markerHighlightRightSide: Boolean = true,
    val xAxisFormatter: AxisLabelFormatter = AxisLabelFormatter { it.toString() },
    val yAxisFormatter: AxisLabelFormatter = AxisLabelFormatter { it.toString() },
) {

    enum class Type {
        Growing,
        Falling,
    }
}