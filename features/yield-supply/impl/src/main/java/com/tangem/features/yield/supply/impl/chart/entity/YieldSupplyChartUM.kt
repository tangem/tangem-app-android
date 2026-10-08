package com.tangem.features.yield.supply.impl.chart.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
internal sealed class YieldSupplyChartUM {

    @Immutable
    data object Loading : YieldSupplyChartUM()

    @Immutable
    data class Error(val onRetry: () -> Unit) : YieldSupplyChartUM()

    /** @property incentive set only while a Merkl campaign is running on the token */
    @Immutable
    data class Data(
        val chartData: YieldSupplyMarketChartDataUM,
        val monthLables: ImmutableList<String>,
        val incentive: YieldSupplyChartIncentiveUM? = null,
    ) : YieldSupplyChartUM()
}

/**
 * @property averageApy period mean of the total APY, null until the history arrives
 * @property onPeriodSelected receives the [YieldSupplyChartPeriod] name carried as the segment id
 */
@Immutable
internal data class YieldSupplyChartIncentiveUM(
    val period: YieldSupplyChartPeriod,
    val picker: TangemSegmentedPickerUM,
    val averageApy: String?,
    val onPeriodSelected: (String) -> Unit,
)

/**
 * @property rewardCaps per-column slice between the base rate and the backend total, stacked on top of [y];
 * empty for the plain Aave chart
 * @property xLabels one entry per column, blank where the axis carries no label
 */
@Immutable
internal data class YieldSupplyMarketChartDataUM(
    val y: ImmutableList<Double>,
    val x: ImmutableList<Double>,
    val avr: Double,
    val percentFormat: String,
    val rewardCaps: ImmutableList<Double> = persistentListOf(),
    val xLabels: ImmutableList<String> = persistentListOf(),
) {
    companion object {
        @Suppress("MagicNumber")
        fun mock(): YieldSupplyMarketChartDataUM {
            val y = listOf(
                4.6, 4.0, 4.2, 3.3, 2.5, 5.6, 4.2, 6.7, 3.5, 2.5, 4.5, 3.4,
                4.6, 4.0, 4.2, 3.3, 2.5, 5.6, 4.2, 6.7, 3.5, 2.5, 4.5, 3.4,
                4.2, 6.7, 3.5, 2.5, 4.5, 3.4,
            ).toImmutableList()
            val x = List(y.size) { 1.0 }.toImmutableList()
            return YieldSupplyMarketChartDataUM(y = y, x = x, avr = 5.15, percentFormat = "%.1f")
        }

        @Suppress("MagicNumber")
        fun mockIncentivised(): YieldSupplyMarketChartDataUM {
            val caps = listOf(
                0.0, 1.4, 1.8, 0.0, 1.2, 2.2, 0.0, 1.6, 1.0, 0.0, 2.0, 1.4,
                0.0, 1.8, 1.2, 0.0, 1.6, 2.4, 0.0, 1.0, 1.8, 0.0, 1.4, 2.0,
                0.0, 1.2, 1.6, 0.0, 2.2, 1.4,
            ).toImmutableList()

            return mock().copy(avr = 6.4, rewardCaps = caps)
        }
    }
}