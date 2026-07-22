package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/** State of the Discovery feed screen. */
@Immutable
internal sealed interface PolymarketMainUM {

    data object Loading : PolymarketMainUM

    data class Content(val events: ImmutableList<PolymarketEventUM>) : PolymarketMainUM

    data object Empty : PolymarketMainUM

    data class Error(val onRetryClick: () -> Unit) : PolymarketMainUM
}

/**
 * A single event card.
 *
 * @property id event id
 * @property title event title, as provided by the backend
 * @property iconUrl optional event icon
 * @property volume formatted total traded volume, `null` when the backend reports none
 * @property rows markets shown on the card — the feed carries the top ones only
 * @property hiddenMarketsCount number of markets not shown on the card, `0` when there are none
 * @property onClick opens the event details
 */
@Immutable
internal data class PolymarketEventUM(
    val id: String,
    val title: TextReference,
    val iconUrl: String?,
    val volume: TextReference?,
    val rows: ImmutableList<PolymarketEventRowUM>,
    val hiddenMarketsCount: Int,
    val onClick: () -> Unit,
)

/**
 * A single market row of an event card.
 *
 * @property marketId market id
 * @property title market label — the market's group item title for grouped events, a generic caption for plain ones
 * @property probability formatted probability of the market's first outcome, `null` when the backend reports none
 * @property outcomes outcome buttons
 */
@Immutable
internal data class PolymarketEventRowUM(
    val marketId: String,
    val title: TextReference,
    val probability: TextReference?,
    val outcomes: ImmutableList<PolymarketOutcomeUM>,
)

/**
 * An outcome button.
 *
 * @property assetId outcome asset id
 * @property title outcome label, as provided by the backend — never a hardcoded "Yes"/"No"
 * @property onClick opens the place-prediction flow for this outcome
 */
@Immutable
internal data class PolymarketOutcomeUM(
    val assetId: String,
    val title: TextReference,
    val onClick: () -> Unit,
)