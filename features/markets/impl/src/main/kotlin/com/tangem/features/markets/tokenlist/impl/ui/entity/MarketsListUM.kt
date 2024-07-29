package com.tangem.features.markets.tokenlist.impl.ui.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.markets.impl.R
import kotlinx.collections.immutable.ImmutableList

internal data class MarketsListUM(
    val list: ListUM,
    val searchBar: SearchBarUM,
    val selectedSortBy: SortByTypeUM,
    val sortByBottomSheet: TangemBottomSheetConfig,
    val selectedInterval: TrendInterval,
    val onIntervalClick: (TrendInterval) -> Unit,
    val onSortByButtonClick: () -> Unit,
) {
    val isInSearchMode
        get() = searchBar.isActive

    enum class TrendInterval(val text: TextReference) {
        H24(resourceReference(R.string.markets_selector_interval_24h_title)),
        D7(resourceReference(R.string.markets_selector_interval_7d_title)),
        M1(resourceReference(R.string.markets_selector_interval_1m_title)),
    }
}

enum class SortByTypeUM(val text: TextReference) {
    Rating(resourceReference(R.string.markets_sort_by_rating_title)),
    Trending(resourceReference(R.string.markets_sort_by_trending_title)),
    ExperiencedBuyers(resourceReference(R.string.markets_sort_by_experienced_buyers_title)),
    TopGainers(resourceReference(R.string.markets_sort_by_top_gainers_title)),
    TopLosers(resourceReference(R.string.markets_sort_by_top_losers_title)),
}

@Immutable
sealed class ListUM {

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
        val showUnder100kTokens: Boolean,
        val loadMore: () -> Unit,
        val visibleIdsChanged: (List<String>) -> Unit,
        val onShowTokensUnder100kClicked: () -> Unit,
        val triggerScrollReset: StateEvent<Unit>,
        val onItemClick: (MarketsListItemUM) -> Unit,
    ) : ListUM()

    data object Loading : ListUM()

    data class LoadingError(
        val onRetryClicked: () -> Unit,
    ) : ListUM()

    data object SearchNothingFound : ListUM()
}
