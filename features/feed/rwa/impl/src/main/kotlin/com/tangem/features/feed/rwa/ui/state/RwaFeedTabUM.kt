package com.tangem.features.feed.rwa.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.ds2.tokenicon.TangemTokenIcon
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.model.list.RwaInterval
import kotlinx.collections.immutable.ImmutableList

/**
 * [appliedSelection] is the source of truth for the next list request's category scope, and
 * [categoriesFilter] is its rendered projection. The trend interval and the sort are owned by the model as
 * `StateFlow`s; [interval] and [sortTabs] only mirror their current values for the chip label, the menu's
 * check mark and the selected tab.
 *
 * @property appliedSelection category filter the list is currently scoped to; empty means "All"
 * @property regionRestriction banner warning that the current scope is blocked in the user's region;
 * `null` hides it
 */
@Immutable
internal data class RwaFeedTabUM(
    val featured: RwaFeaturedUM,
    val categoriesFilter: TangemFilterItemUM,
    val regionRestriction: TangemMessageBanner.State?,
    val interval: RwaIntervalUM,
    val sortTabs: ImmutableList<TangemTabItemUM>,
    val onVisibleItemsChange: (List<String>) -> Unit,
    val list: RwaListUM,
    val appliedSelection: CategorySelection,
)

/**
 * Trend-interval chip and its menu. Carries no option list — the menu renders `RwaInterval.entries` — and
 * no expanded flag, which is local Compose state in the anchor.
 */
@Immutable
internal data class RwaIntervalUM(
    val selected: RwaInterval,
    val default: RwaInterval,
    val onOptionClick: (RwaInterval) -> Unit,
)

@Immutable
internal sealed interface RwaFeaturedUM {

    data object Loading : RwaFeaturedUM

    @Immutable
    data class Content(val assets: ImmutableList<RwaFeaturedAssetUM>) : RwaFeaturedUM

    /** Nothing to show — a failed fetch or a backend without the `featured-assets` category. */
    data object Empty : RwaFeaturedUM
}

@Immutable
internal data class RwaFeaturedAssetUM(
    val id: String,
    val iconUrl: String,
    val indicator: TangemTokenIcon.State.Indicator?,
    val name: TextReference,
    val ticker: TextReference,
    val price: TextReference,
    val priceChange: TangemPriceChange.State,
    val onClick: () -> Unit,
)

@Immutable
internal sealed interface RwaListUM {

    /** Initial load, or a reload after a filter / sort / interval change — shimmer rows. */
    data object Loading : RwaListUM

    @Immutable
    data class Content(
        val items: ImmutableList<RwaListItemUM>,
        val loadMore: () -> Unit,
    ) : RwaListUM

    @Immutable
    data class Error(val onRetry: () -> Unit) : RwaListUM
}

/** @property chartData mini-chart series; `null` while the chart batch is still loading */
@Immutable
internal data class RwaListItemUM(
    val row: TangemTokenRowMarket.State.Content,
    val chartData: MarketChartRawData?,
    val chartType: MarketChartLook.Type,
) {
    val id: String get() = row.id
}