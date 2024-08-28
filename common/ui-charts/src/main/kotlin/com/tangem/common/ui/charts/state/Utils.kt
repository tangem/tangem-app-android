package com.tangem.common.ui.charts.state

import kotlinx.collections.immutable.toImmutableList

fun MarketChartData.Data.sorted(): MarketChartData.Data {
    val points = this.x.zip(this.y).sortedBy { it.first }

    return MarketChartData.Data(
        x = points.map { it.first }.toImmutableList(),
        y = points.map { it.second }.toImmutableList(),
    )
}
