package com.tangem.features.yield.supply.impl.chart.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
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
        val incentive: YieldSupplyChartIncentiveUM? = null,
    ) : YieldSupplyChartUM()
}

/**
 * @property averageApy period mean of the total APY; a dash until the history arrives
 * @property onPeriodSelected receives the [YieldSupplyChartPeriod] name carried as the segment id
 */
@Immutable
internal data class YieldSupplyChartIncentiveUM(
    val period: YieldSupplyChartPeriod,
    val averageApy: TextReference,
    val onPeriodSelected: (String) -> Unit,
)

/**
 * @property rewardCaps per-column slice between the base rate and the backend total, stacked on top of [y];
 * empty for the plain Aave chart
 * @property scale vertical axis range and grid step, sized to the tallest column
 */
@Immutable
internal data class YieldSupplyMarketChartDataUM(
    val y: ImmutableList<Double>,
    val x: ImmutableList<Double>,
    val avr: Double,
    val scale: YieldSupplyChartScale,
    val rewardCaps: ImmutableList<Double> = persistentListOf(),
    val xLabels: ImmutableList<YieldSupplyChartAxisLabel> = persistentListOf(),
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
            val xLabels = listOf(5.6 to "Jan", 13.2 to "Apr", 20.8 to "Jul", 28.4 to "Oct").map { (position, text) ->
                YieldSupplyChartAxisLabel(
                    text = text,
                    position = position,
                    anchor = YieldSupplyChartAxisLabel.Anchor.Start,
                )
            }.toImmutableList()
            return YieldSupplyMarketChartDataUM(
                y = y,
                x = x,
                avr = 5.15,
                scale = YieldSupplyChartScale(max = 9.0, step = 3.0, decimals = 0),
                xLabels = xLabels,
            )
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

/**
 * @property max top of the vertical axis, a whole number of [step]s at or above the tallest column
 * @property decimals fraction digits a grid label needs to write [step] out
 */
@Immutable
internal data class YieldSupplyChartScale(val max: Double, val step: Double, val decimals: Int)

/**
 * @property position in columns from the start of the plot: a [Anchor.Start] label begins there, a [Anchor.Center]
 * one is centred on it
 */
@Immutable
internal data class YieldSupplyChartAxisLabel(val text: String, val position: Double, val anchor: Anchor) {
    enum class Anchor { Start, Center }
}