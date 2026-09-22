package com.tangem.features.polymarket.impl.details.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.marketprice.PriceChangeState
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the event-details screen.
 */
@Immutable
internal sealed interface PolymarketEventDetailsUM {

    data object Loading : PolymarketEventDetailsUM

    data class Error(val onRetryClick: () -> Unit) : PolymarketEventDetailsUM

    /**
     * @property title event title, shown large in the scrolling header and single-line in the collapsed bar
     * @property iconUrl optional event icon
     * @property totalVolume formatted total traded volume, shown in the meta row under the title;
     *  a dash when unknown
     * @property change24h total-volume growth over the last 24 hours as a price change,
     *  [PriceChangeState.Unknown] when the volumes needed to derive it are missing
     * @property subcategories market-filter tabs under the meta row; empty hides the row
     *  (the BFF does not serve subcategories yet)
     * @property activeMarkets markets open for trading
     * @property closedMarkets resolved markets, hidden behind the "Closed Markets" chip
     * @property isClosedMarketsExpanded whether the closed markets are unfolded under the chip
     * @property description resolution criteria text, `null` when blank
     * @property isDescriptionExpanded whether the description shows in full or clipped with "Read more"
     * @property resolutionDate formatted resolution date of the event, a dash when unknown
     * @property marketOpenedDate formatted opening date of the event, a dash when unknown
     * @property onShareClick share the event link
     * @property onClosedMarketsClick toggle [isClosedMarketsExpanded]
     * @property onReadMoreClick expand the description
     */
    @Suppress("LongParameterList")
    data class Content(
        val title: TextReference,
        val iconUrl: String?,
        val totalVolume: TextReference,
        val change24h: PriceChangeState,
        val subcategories: ImmutableList<PolymarketSubcategoryTabUM>,
        val activeMarkets: ImmutableList<PolymarketDetailsMarketUM>,
        val closedMarkets: ImmutableList<PolymarketDetailsMarketUM>,
        val isClosedMarketsExpanded: Boolean,
        val description: TextReference?,
        val isDescriptionExpanded: Boolean,
        val resolutionDate: TextReference,
        val marketOpenedDate: TextReference,
        val onShareClick: () -> Unit,
        val onClosedMarketsClick: () -> Unit,
        val onReadMoreClick: () -> Unit,
    ) : PolymarketEventDetailsUM
}

/**
 * A market-filter tab of the event-details screen (e.g. "Game lines" / "Exact score").
 *
 * @property id subcategory id, used as the lazy-row key
 * @property label localized display name, as provided by the backend
 * @property isSelected whether the tab's subcategory is the one currently shown
 * @property onClick selects the tab and filters the markets list
 */
@Immutable
internal data class PolymarketSubcategoryTabUM(
    val id: String,
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

/**
 * A single market card of the event-details screen.
 *
 * @property id market id, used as the lazy-list key
 * @property title market label — the group item title for grouped events, the market question otherwise
 * @property volume formatted traded volume of the market, a dash when unknown
 * @property iconUrl optional market icon
 * @property outcomes outcome buttons; empty for closed markets, which are no longer tradable
 */
@Immutable
internal data class PolymarketDetailsMarketUM(
    val id: String,
    val title: TextReference,
    val volume: TextReference,
    val iconUrl: String?,
    val outcomes: ImmutableList<PolymarketDetailsOutcomeUM>,
)

/**
 * An outcome button of a market card.
 *
 * @property assetId upstream asset id of the outcome
 * @property title outcome label as served by the backend
 * @property price outcome share price, e.g. "25¢", or a dash when unknown
 * @property onClick opens the Place-prediction sheet preselected with this outcome
 */
@Immutable
internal data class PolymarketDetailsOutcomeUM(
    val assetId: String,
    val title: TextReference,
    val price: TextReference,
    val onClick: () -> Unit,
)