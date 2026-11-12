package com.tangem.features.polymarket.impl.common.feed.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.polymarket.model.PolymarketCategoryIcon
import com.tangem.features.polymarket.impl.common.PolymarketOutcomeAccent
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal sealed interface PolymarketFeedContentUM {

    data object Loading : PolymarketFeedContentUM

    data class Content(
        val events: ImmutableList<PolymarketEventUM>,
        val footer: PolymarketFeedFooterUM,
    ) : PolymarketFeedContentUM

    /** The category has no events: the first page came back empty and ended the feed. */
    data object Empty : PolymarketFeedContentUM

    /** A failed request — of the categories or of the first page; the prompt retries whichever failed. */
    data class Error(val onReloadClick: () -> Unit) : PolymarketFeedContentUM
}

@Immutable
internal sealed interface PolymarketFeedFooterUM {

    data object None : PolymarketFeedFooterUM

    data object Loading : PolymarketFeedFooterUM

    /** Takes a tap as well as a scroll: a list already at its end has nowhere to scroll to. */
    data class Error(val onRetryClick: () -> Unit) : PolymarketFeedFooterUM
}

@Immutable
internal data class PolymarketFeedScrollPosition(val index: Int, val offset: Int)

/** @property icon drawn before the label, `null` shows the label alone */
@Immutable
internal data class PolymarketCategoryTabUM(
    val id: Int,
    val label: TextReference,
    val icon: PolymarketCategoryIcon?,
    val isSelected: Boolean,
    val onClick: () -> Unit,
)

@Immutable
internal data class PolymarketEventUM(
    val id: String,
    val title: TextReference,
    val iconUrl: String?,
    val volume: TextReference,
    val rows: ImmutableList<PolymarketEventRowUM>,
    val hiddenMarketsCount: Int,
    val onClick: () -> Unit,
    val sports: PolymarketSportsUM? = null,
)

/**
 * @property title market label, omitted when the event title already identifies a market shown with prices
 * @property probability `null` shows full-width outcome buttons with prices instead of a probability column
 */
@Immutable
internal data class PolymarketEventRowUM(
    val marketId: String,
    val title: TextReference?,
    val probability: TextReference?,
    val outcomes: ImmutableList<PolymarketOutcomeUM>,
)

/** @property title as provided by the backend — never a hardcoded "Yes"/"No" */
@Immutable
internal data class PolymarketOutcomeUM(
    val assetId: String,
    val title: TextReference,
    val onClick: () -> Unit,
    val price: TextReference? = null,
    val accent: PolymarketOutcomeAccent? = null,
)