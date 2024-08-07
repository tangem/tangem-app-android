package com.tangem.common.ui.charts.state

import androidx.compose.runtime.Immutable

@Immutable
data class MarketChartRawData(
    val y: List<Float>,
    val x: List<Float> = List(y.size) { 1f },
)
