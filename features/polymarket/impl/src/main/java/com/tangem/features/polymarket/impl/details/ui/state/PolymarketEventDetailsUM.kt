package com.tangem.features.polymarket.impl.details.ui.state

import androidx.compose.runtime.Immutable
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
     *  `null` when unknown
     * @property change24h formatted total-volume growth over the last 24 hours (e.g. "2.08%"),
     *  `null` when the volumes needed to derive it are unknown
     * @property subcategories market-filter tabs under the meta row; empty hides the row
     *  (the BFF does not serve subcategories yet)
     * @property activeMarkets markets open for trading
     * @property onShareClick share the event link
     */
    data class Content(
        val title: TextReference,
        val iconUrl: String?,
        val totalVolume: TextReference?,
        val change24h: TextReference?,
        val subcategories: ImmutableList<PolymarketSubcategoryTabUM>,
        val activeMarkets: ImmutableList<PolymarketDetailsMarketUM>,
        val onShareClick: () -> Unit,
    ) : PolymarketEventDetailsUM
}

/**
 * A market-filter tab of the event-details sheet (e.g. "Game lines" / "Exact score").
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
 * @property volume formatted traded volume of the market, `null` when unknown
 * @property iconUrl optional market icon
 * @property outcomes outcome buttons of a tradable market
 */
@Immutable
internal data class PolymarketDetailsMarketUM(
    val id: String,
    val title: TextReference,
    val volume: TextReference?,
    val iconUrl: String?,
    val outcomes: ImmutableList<PolymarketDetailsOutcomeUM>,
)

/**
 * An outcome button of a market card, labelled with the outcome and its price (e.g. "Yes • 25¢").
 *
 * @property assetId upstream asset id of the outcome
 * @property title button label
 * @property onClick opens the Place-prediction sheet preselected with this outcome
 */
@Immutable
internal data class PolymarketDetailsOutcomeUM(
    val assetId: String,
    val title: TextReference,
    val onClick: () -> Unit,
)