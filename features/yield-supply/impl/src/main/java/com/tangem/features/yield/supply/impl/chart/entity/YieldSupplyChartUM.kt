package com.tangem.features.yield.supply.impl.chart.entity

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
internal sealed class YieldSupplyChartUM {

    @Immutable
    data object Loading : YieldSupplyChartUM()

    @Immutable
    data class Error(val onRetry: () -> Unit) : YieldSupplyChartUM()

    @Immutable
    data class Data(val chartData: YieldSupplyMarketChartData) : YieldSupplyChartUM()
}

@Immutable
internal data class YieldSupplyMarketChartData(
    val y: ImmutableList<Double>,
    val x: ImmutableList<Double>,
) {
    companion object {
        @Suppress("MagicNumber")
        fun mock(): YieldSupplyMarketChartData {
            // TODO [REDACTED_TASK_KEY] replace mock values with real APY series
            val y = listOf(
                4.6, 4.0, 4.2, 3.3, 2.5, 5.6, 4.2, 6.7, 3.5, 2.5, 4.5, 3.4,
            ).toImmutableList()
            val x = List(y.size) { 1.0 }.toImmutableList()
            return YieldSupplyMarketChartData(y = y, x = x)
        }
    }
}