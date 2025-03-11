package com.tangem.common.ui.charts.state.converter

import com.tangem.common.ui.charts.state.MarketChartData
import com.tangem.common.ui.charts.state.MarketChartRawData
import java.math.BigDecimal

/**
 * Interface to convert chart data values to Floats and backwards.
 *
 * We need to convert the values on the graph to floating point values in order to display them correctly on the canvas.
 * We also need to determine exactly which floating point value on the graph corresponds to the decimal point,
 * so that we can format the actual value and display on the x/y axis.
 *
 * **[prepareRawXForFormat] and [prepareRawYForFormat] must be very fast because they are called in the onDraw method**
 */
interface PointValuesConverter {

    fun convert(data: MarketChartData.Data): MarketChartRawData

    fun prepareRawXForFormat(rawX: Double, data: MarketChartData.Data): BigDecimal

    fun prepareRawYForFormat(rawY: Double, data: MarketChartData.Data): BigDecimal
}