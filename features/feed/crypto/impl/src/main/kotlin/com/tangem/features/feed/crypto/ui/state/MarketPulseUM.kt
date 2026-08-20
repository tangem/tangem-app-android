package com.tangem.features.feed.crypto.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/** UI model of the crypto feed tab: the Total market cap block and the Market Pulse list. */
@Immutable
internal data class CryptoFeedTabUM(
    val totalMarketCap: TotalMarketCapUM,
    val marketPulse: MarketPulseUM,
)

/** Stub "Total market cap" block: headline value with a change badge and two market index cards. */
@Immutable
internal data class TotalMarketCapUM(
    val title: TextReference,
    val value: TextReference,
    val change: TangemPriceChange.State,
    val indexes: ImmutableList<MarketIndexCardUM>,
)

/**
 * A market index card of the Total market cap block (Fear & greed, Altcoin index).
 *
 * @param progress index position on the gradient scale, `0f..1f`
 */
@Immutable
internal data class MarketIndexCardUM(
    val id: String,
    val title: TextReference,
    val statusLabel: TextReference,
    val isGrowth: Boolean,
    val value: TextReference,
    val progress: Float,
)

/** UI model of the Market Pulse block. */
@Immutable
internal data class MarketPulseUM(
    val title: TextReference,
    val interval: TangemFilterItemUM,
    val categories: ImmutableList<MarketPulseCategoryUM>,
    val selectedCategoryIndex: Int,
    val onCategorySelect: (Int) -> Unit,
    val list: MarketPulseListUM,
    val onVisibleItemsChange: (List<String>) -> Unit,
)

@Immutable
internal data class MarketPulseCategoryUM(
    val id: String,
    val title: TextReference,
)

/** State of the Market Pulse markets list. */
@Immutable
internal sealed interface MarketPulseListUM {

    /** Initial load (or reload after a sort/interval change) — shimmer rows. */
    data object Loading : MarketPulseListUM

    data class Content(
        val items: ImmutableList<MarketPulseItemUM>,
        val loadMore: () -> Unit,
    ) : MarketPulseListUM

    data class Error(val onRetry: () -> Unit) : MarketPulseListUM
}

/**
 * A single markets-list row.
 *
 * @param chartData mini-chart series; `null` while the chart batch is still loading
 */
@Immutable
internal data class MarketPulseItemUM(
    val row: TangemTokenRowMarket.State.Content,
    val chartData: MarketChartRawData?,
    val chartType: MarketChartLook.Type,
) {
    val id: String get() = row.id
}