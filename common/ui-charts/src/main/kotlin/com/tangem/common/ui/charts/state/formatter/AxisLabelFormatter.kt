package com.tangem.common.ui.charts.state.formatter

import androidx.compose.runtime.Stable
import java.math.BigDecimal

/**
 * Used for formatting the axis labels in a chart.
 * It takes a BigDecimal value and returns a CharSequence that represents the formatted label.
 *
 * [format] has to be very fast because it is called in the onDraw method.
 *
 * @param value The value to be formatted.
 * @return The formatted label as a CharSequence.
 */
@Stable
fun interface AxisLabelFormatter {

    fun format(value: BigDecimal): CharSequence
}
