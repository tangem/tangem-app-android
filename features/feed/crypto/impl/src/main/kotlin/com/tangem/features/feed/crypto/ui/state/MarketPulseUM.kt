package com.tangem.features.feed.crypto.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.features.feed.crypto.model.list.MarketPulseInterval
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

/** State of the "Total market cap" block. */
@Immutable
internal sealed interface TotalMarketCapUM {

    /** First load — shimmer placeholders repeating the [Content] geometry. */
    data object Loading : TotalMarketCapUM

    /** Nothing to show: the backend published no indicators, or they could not be loaded at all. */
    data object Empty : TotalMarketCapUM

    /**
     * Headline capitalisation with a change badge and a row of market index cards.
     *
     * @param indexes      holds fewer than two cards when the backend omits an index
     * @param onIndexClick opens the info sheet for the tapped index; has to stay a bound reference so
     *                     that the model's `distinctUntilChanged` still dedupes equal states
     */
    data class Content(
        val title: TextReference,
        val value: TextReference,
        val change: TangemPriceChange.State,
        val indexes: ImmutableList<MarketIndexUM>,
        val onIndexClick: (MarketIndexUM) -> Unit,
        val onInfoClick: () -> Unit,
    ) : TotalMarketCapUM
}

/**
 * A market index of the Total market cap block (Fear & greed, Altcoin index).
 *
 * Doubles as the payload of its info sheet, which is an expanded view of the same reading — hence
 * [fullTitle] and [description], which only the sheet renders.
 *
 * @param title    short name, for the card where two of them sit side by side
 * @param progress index position on the gradient scale, `0f..1f`
 */
@Immutable
internal data class MarketIndexUM(
    val id: String,
    val title: TextReference,
    val fullTitle: TextReference,
    val description: TextReference,
    val statusLabel: TextReference,
    val status: MarketIndexStatus,
    val value: TextReference,
    val progress: Float,
) : TangemBottomSheetConfigContent

/** Band a market index reading falls into, resolved to a `colors3.text.status` colour by the UI. */
internal enum class MarketIndexStatus {
    Error,
    Warning,
    Success,
    Info,
}

/** UI model of the Market Pulse block. */
@Immutable
internal data class MarketPulseUM(
    val title: TextReference,
    val categoriesFilter: TangemFilterItemUM?,
    val intervalMenu: MarketPulseIntervalUM,
    /** Sort tabs. Not to be confused with [categoriesFilter], which scopes the list to a Gecko category. */
    val sortTabs: ImmutableList<TangemTabItemUM>,
    val list: MarketPulseListUM,
    val onVisibleItemsChange: (List<String>) -> Unit,
)

@Immutable
internal data class MarketPulseIntervalUM(
    val selected: MarketPulseInterval,
    val default: MarketPulseInterval,
    val onOptionClick: (MarketPulseInterval) -> Unit,
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
 * @param isUnderMarketCapLimit low-capitalisation token, hidden behind a notification in search results
 */
@Immutable
internal data class MarketPulseItemUM(
    val row: TangemTokenRowMarket.State.Content,
    val chartData: MarketChartRawData?,
    val chartType: MarketChartLook.Type,
    val isUnderMarketCapLimit: Boolean = false,
) {
    val id: String get() = row.id
}