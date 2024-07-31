package com.tangem.common.ui.charts.layer

import com.patrykandpatrick.vico.core.cartesian.CartesianDrawContext
import com.patrykandpatrick.vico.core.cartesian.CartesianMeasureContext
import com.patrykandpatrick.vico.core.cartesian.HorizontalDimensions
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.data.ChartValues

@Suppress("MagicNumber")
class TimeItemPlacer : HorizontalAxis.ItemPlacer {

    private val ChartValues.measuredLabelValues
        get() = buildList {
            // produce exactly 6 values distributed evenly
            val xLength = maxX - minX
            val xStep = xLength / 7

            repeat(times = 6) {
                add(minX + xStep * (it + 1))
            }
        }

    override fun getEndHorizontalAxisInset(
        context: CartesianMeasureContext,
        horizontalDimensions: HorizontalDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f

    override fun getStartHorizontalAxisInset(
        context: CartesianMeasureContext,
        horizontalDimensions: HorizontalDimensions,
        tickThickness: Float,
        maxLabelWidth: Float,
    ): Float = 0f

    override fun getHeightMeasurementLabelValues(
        context: CartesianMeasureContext,
        horizontalDimensions: HorizontalDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = context.chartValues.measuredLabelValues

    override fun getLabelValues(
        context: CartesianDrawContext,
        visibleXRange: ClosedFloatingPointRange<Double>,
        fullXRange: ClosedFloatingPointRange<Double>,
        maxLabelWidth: Float,
    ): List<Double> = context.chartValues.measuredLabelValues

    override fun getWidthMeasurementLabelValues(
        context: CartesianMeasureContext,
        horizontalDimensions: HorizontalDimensions,
        fullXRange: ClosedFloatingPointRange<Double>,
    ): List<Double> = context.chartValues.measuredLabelValues
}
