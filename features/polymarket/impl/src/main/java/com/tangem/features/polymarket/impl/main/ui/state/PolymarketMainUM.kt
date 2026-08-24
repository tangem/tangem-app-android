package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State of the Discovery feed screen.
 *
 * @property categories category tabs, shown above [content] and kept visible while the events of the selected
 *  category reload or fail. Empty when the backend could not serve them — the feed then runs unfiltered.
 * @property content the events area, which changes independently of [categories]
 */
@Immutable
internal data class PolymarketMainUM(
    val categories: ImmutableList<PolymarketCategoryTabUM>,
    val content: ContentUM,
) {

    @Immutable
    sealed interface ContentUM {

        data object Loading : ContentUM

        /**
         * @property events events of the selected category, accumulated over the loaded pages
         * @property isLoadingNextPage whether the next page is on its way, shown as a footer loader
         */
        data class Content(
            val events: ImmutableList<PolymarketEventUM>,
            val isLoadingNextPage: Boolean,
        ) : ContentUM

        /**
         * Events could not be served: the request failed, or the category came back empty — the design shows
         * one and the same reload prompt for both.
         *
         * @property onReloadClick retries the events (and the categories, when those failed too)
         */
        data class Error(val onReloadClick: () -> Unit) : ContentUM
    }
}

/**
 * A single category tab of the Discovery feed.
 *
 * @property id category id, used to filter the events feed
 * @property label localized display name, as provided by the backend
 * @property isSelected whether the tab's category is the one currently shown
 * @property onClick selects the tab and reloads the feed for its category
 */
@Immutable
internal data class PolymarketCategoryTabUM(
    val id: Int,
    val label: String,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

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