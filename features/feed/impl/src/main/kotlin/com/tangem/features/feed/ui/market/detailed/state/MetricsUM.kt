package com.tangem.features.feed.ui.market.detailed.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class MetricsUM(
    val rows: ImmutableList<Row>,
) {

    internal data class Row(
        val first: MetricItemUM,
        val second: MetricItemUM?,
    )
}

@Immutable
internal sealed interface MetricItemUM {

    @Immutable
    data class MarketCap(
        val capitalizationValue: TextReference?,
        val onInfoClick: () -> Unit,
    ) : MetricItemUM

    @Immutable
    data class TradingVolume(
        val tradingValue: TextReference?,
        val liquidity: Float?,
        val trendingVolumeLiquidityType: TrendingVolumeLiquidityType,
        val onInfoClick: () -> Unit,
    ) : MetricItemUM

    @Immutable
    data class MarketPosition(
        val position: TextReference?,
        val rangeValue: Float?,
        val marketRatingChange24H: MarketRatingChange24H,
        val marketRatingType: MarketRatingType,
        val onInfoClick: () -> Unit,
    ) : MetricItemUM

    @Immutable
    data class FullyDilutedValuation(
        val value: TextReference?,
        val fullyDilutedValuationChange24: TextReference?,
        val onInfoClick: () -> Unit,
    ) : MetricItemUM

    @Immutable
    data class CirculatingSupply(
        val currentValue: TextReference?,
        val maxValue: TextReference?,
        val fillValue: Float?,
        val onInfoClick: () -> Unit,
    ) : MetricItemUM
}

internal enum class TrendingVolumeLiquidityType {
    HIGH, MEDIUM, LOW, UNKNOWN
}

internal enum class MarketRatingType {
    GOLD, SILVER, BRONZE, OTHER
}

internal sealed interface MarketRatingChange24H {

    data class Up(val changeValue: Int) : MarketRatingChange24H

    data class Down(val changeValue: Int) : MarketRatingChange24H

    data object NoChanges : MarketRatingChange24H
}