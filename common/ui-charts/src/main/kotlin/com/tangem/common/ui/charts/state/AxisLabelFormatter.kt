package com.tangem.common.ui.charts.state

import java.math.BigDecimal

/**
 * Used for formatting the axis labels in a chart.
 * It takes a BigDecimal value and returns a CharSequence that represents the formatted label.
 *
 * @param value The value to be formatted.
 * @return The formatted label as a CharSequence.
 */
fun interface AxisLabelFormatter {

    fun format(value: BigDecimal): CharSequence
}
