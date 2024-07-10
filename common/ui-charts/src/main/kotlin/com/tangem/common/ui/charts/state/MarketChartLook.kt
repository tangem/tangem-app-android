package com.tangem.common.ui.charts.state

/**
 * This class represents the look and feel of a Market Chart.
 * It includes properties for type, marker highlight, animation on data change, animate data appearance,
 * and formatters for x and y axis.
 *
 * @property type The type of the chart, can be either Growing or Falling.
 * @property markerHighlightRightSide A boolean indicating whether the marker highlights the right side of the chart.
 * @property animationOnDataChange A boolean indicating whether to animate on data change.
 * @property animateDataAppearance A boolean indicating whether to animate data appearance.
 * @property xAxisFormatter A formatter for the x-axis labels.
 * @property yAxisFormatter A formatter for the y-axis labels.
 */
data class MarketChartLook(
    val type: Type = Type.Growing,
    val markerHighlightRightSide: Boolean = true,
    val animationOnDataChange: Boolean = false,
    val animateDataAppearance: Boolean = false,
    val xAxisFormatter: AxisLabelFormatter = AxisLabelFormatter { it.toString() },
    val yAxisFormatter: AxisLabelFormatter = AxisLabelFormatter { it.toString() },
) {

    enum class Type {
        Growing,
        Falling,
    }
}