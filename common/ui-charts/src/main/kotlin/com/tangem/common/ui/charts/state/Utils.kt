package com.tangem.common.ui.charts.state

import kotlinx.collections.immutable.toImmutableList

fun MarketChartData.Data.sorted(): MarketChartData.Data {
    val points = this.x.zip(this.y).sortedBy { it.first }

    return MarketChartData.Data(
        points.map { it.first }.toImmutableList(),
        points.map { it.second }.toImmutableList(),
    )
}
