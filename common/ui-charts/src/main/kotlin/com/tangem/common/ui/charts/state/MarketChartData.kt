package com.tangem.common.ui.charts.state

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

@Immutable
sealed interface MarketChartData {

    /**
     * This interface represents the state when there is no data for the Market Chart.
     */
    @Immutable
    sealed interface NoData : MarketChartData {
        @Immutable
        data object Empty : NoData

        @Immutable
        data object Loading : NoData

        @Immutable
        data object ErrorAndRetry : NoData
    }

    /**
     * This data class represents the data for the Market Chart.
     * It includes properties for x and y values.
     *
     * @property x List of x values.
     * @property y List of y values.
     */
    @Immutable
    data class Data(
        val x: ImmutableList<BigDecimal> = persistentListOf(),
        val y: ImmutableList<BigDecimal> = persistentListOf(),
    ) : MarketChartData
}