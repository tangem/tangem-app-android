package com.tangem.common.ui.charts.state

import kotlinx.collections.immutable.toImmutableList

fun MarketChartData.Data.sorted(): MarketChartData.Data {
    val points = this.x.zip(this.y).sortedBy { it.first }
    val (x, y) = points.unzip()

    return MarketChartData.Data(
        x = x.toImmutableList(),
        y = y.toImmutableList(),
    )
}