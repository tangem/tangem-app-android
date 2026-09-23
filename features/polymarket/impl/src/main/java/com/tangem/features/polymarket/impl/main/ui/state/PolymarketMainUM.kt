package com.tangem.features.polymarket.impl.main.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * @property categories empty when the backend could not serve them — the feed then runs unfiltered
 * @property pendingScrollPosition applied by the screen once the returned-to category is laid out, then cleared
 */
@Immutable
internal data class PolymarketMainUM(
    val balance: PredictionBalanceUM,
    val categories: ImmutableList<PolymarketCategoryTabUM>,
    val content: ContentUM,
    val pullToRefreshConfig: PullToRefreshConfig,
    val pendingScrollPosition: PolymarketFeedScrollPosition? = null,
    val positions: PolymarketPositionsPreviewUM = PolymarketPositionsPreviewUM.Loading,
    val isAwaitingFunds: Boolean = false,
    val onActivePositionsClick: (() -> Unit)? = null,
    val onFinishedPositionsClick: (() -> Unit)? = null,
) {

    @Immutable
    sealed interface ContentUM {

        data object Loading : ContentUM

        data class Content(
            val events: ImmutableList<PolymarketEventUM>,
            val footer: PolymarketFeedFooterUM,
        ) : ContentUM

        /** Also an empty category: the design shows one reload prompt for both. */
        data class Error(val onReloadClick: () -> Unit) : ContentUM
    }
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

@Immutable
internal data class PolymarketCategoryTabUM(
    val id: Int,
    val label: String,
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
)

/** @property title the market's group item title for grouped events, a generic caption for plain ones */
@Immutable
internal data class PolymarketEventRowUM(
    val marketId: String,
    val title: TextReference,
    val probability: TextReference,
    val outcomes: ImmutableList<PolymarketOutcomeUM>,
)

/** @property title as provided by the backend — never a hardcoded "Yes"/"No" */
@Immutable
internal data class PolymarketOutcomeUM(
    val assetId: String,
    val title: TextReference,
    val onClick: () -> Unit,
)