package com.tangem.common.ui.charts.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * This class represents raw data for a Market Chart. Used for drawing the chart.
 *
 * @property originalIndexes If the source data has the original representation (due to reduced sampling),
 * this list contains the original indexes of the data points.
 * @property y The list of y-values.
 * @property x The list of x-values.
 */
@Immutable
data class MarketChartRawData(
    val originalIndexes: ImmutableList<Int>? = null,
    val y: ImmutableList<Double>,
    val x: ImmutableList<Double> = List(y.size) { 1.0 }.toImmutableList(),
)