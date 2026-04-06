package com.tangem.features.feed.ui.market.detailed.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class MetricsUM(
    val metrics: ImmutableList<InfoPointUM>,
    val metricsV2: MetricsV2UM?,
)

@Immutable
internal sealed interface InfoPointUMV2 {

    @Immutable
    data class MarketCap(
        val capitalizationValue: TextReference?,
        val onInfoClick: () -> Unit,
    ) : InfoPointUMV2

    @Immutable
    data class TradingVolume(
        val tradingValue: TextReference?,
        val liquidity: Float?,
        val trendingVolumeLiquidityType: TrendingVolumeLiquidityType,
        val onInfoClick: () -> Unit,
    ) : InfoPointUMV2

    @Immutable
    data class MarketPosition(
        val position: TextReference?,
        val rangeValue: Float?,
        val marketRatingChange24H: MarketRatingChange24H,
        val marketRatingType: MarketRatingType,
        val onInfoClick: () -> Unit,
    ) : InfoPointUMV2

    @Immutable
    data class FullyDilutedValuation(
        val value: TextReference?,
        val fullyDilutedValuationChange24: TextReference?,
        val onInfoClick: () -> Unit,
    ) : InfoPointUMV2

    @Immutable
    data class CirculatingSupply(
        val currentValue: TextReference?,
        val maxValue: TextReference?,
        val fillValue: Float?,
        val onInfoClick: () -> Unit,
    ) : InfoPointUMV2
}

internal data class MetricsV2UM(
    val rows: ImmutableList<Row>,
) {

    internal data class Row(
        val first: InfoPointUMV2,
        val second: InfoPointUMV2?,
    )
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