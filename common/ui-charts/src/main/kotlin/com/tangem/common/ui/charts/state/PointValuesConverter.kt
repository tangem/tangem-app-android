package com.tangem.common.ui.charts.state

import java.math.BigDecimal

/**
 * Interface to convert chart data values to Floats and backwards.
 *
 * We need to convert the values on the graph to floating point values in order to display them correctly on the canvas.
 * We also need to determine exactly which floating point value on the graph corresponds to the decimal point,
 * so that we can format the actual value and display on the x/y axis.
 */
interface PointValuesConverter {

    fun convert(data: MarketChartData.Data): MarketChartRawData

    fun prepareRawXForFormat(rawX: Float, data: MarketChartData.Data): BigDecimal

    fun prepareRawYForFormat(rawY: Float, data: MarketChartData.Data): BigDecimal
}

object DefaultPointValuesConverter : PointValuesConverter {

    override fun convert(data: MarketChartData.Data): MarketChartRawData {
        val minX = data.x.min()
        val minY = data.y.min()

        val normY = data.y.map { normalize(it, minY) }
        val normX = data.x.map { normalize(it, minX) }

        return MarketChartRawData(
            x = normX,
            y = normY,
        )
    }

    override fun prepareRawXForFormat(rawX: Float, data: MarketChartData.Data): BigDecimal {
        val dataMin = data.x.min()
        val scale = dataMin.scale()
        val bVal = if (scale > 2) {
            rawX.toBigDecimal().movePointLeft(scale - 2) + dataMin
        } else {
            rawX.toBigDecimal() + dataMin
        }

        return bVal
    }

    override fun prepareRawYForFormat(rawY: Float, data: MarketChartData.Data): BigDecimal {
        val dataMin = data.y.min()
        val scale = dataMin.scale()
        val bVal = if (scale > 2) {
            rawY.toBigDecimal().movePointLeft(scale - 2) + dataMin
        } else {
            rawY.toBigDecimal() + dataMin
        }

        return bVal
    }

    // TODO enhance algorithm for values with big difference between min and max, which cannot fit in Float
    private fun normalize(value: BigDecimal, min: BigDecimal, scale: Int = min.scale()): Float {
        val n = value - min
        return if (scale > 2) {
            n.movePointRight(scale - 2).toFloat()
        } else {
            n.toFloat()
        }
    }
}