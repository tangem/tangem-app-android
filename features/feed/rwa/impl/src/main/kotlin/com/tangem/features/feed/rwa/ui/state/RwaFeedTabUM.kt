package com.tangem.features.feed.rwa.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.charts.state.MarketChartLook
import com.tangem.common.ui.charts.state.MarketChartRawData
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.ds2.segmentedcontrol.TangemSegmentedControl
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.core.ui.ds2.util.TangemPriceChange
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.model.list.RwaSort
import kotlinx.collections.immutable.ImmutableList

/**
 * [selectedInterval], [selectedSort] and [appliedSelection] are the source of truth for the next list
 * request; [interval] and [categoriesFilter] are the rendered projections of the latter two.
 *
 * @property appliedSelection category filter the list is currently scoped to; empty means "All"
 */
@Immutable
internal data class RwaFeedTabUM(
    val featured: RwaFeaturedUM,
    val categoriesFilter: TangemFilterItemUM,
    val interval: TangemSegmentedControl.State,
    val tags: ImmutableList<RwaTagUM>,
    val onTagSelect: (Int) -> Unit,
    val onVisibleItemsChange: (List<String>) -> Unit,
    val list: RwaListUM,
    val selectedInterval: RwaInterval,
    val selectedSort: RwaSort,
    val appliedSelection: CategorySelection,
) {

    /** [tags] is built from `RwaSort.entries`, so the selected sort is the selected pill. */
    val selectedTagIndex: Int get() = RwaSort.entries.indexOf(selectedSort)
}

@Immutable
internal sealed interface RwaFeaturedUM {

    data object Loading : RwaFeaturedUM

    @Immutable
    data class Content(val assets: ImmutableList<RwaFeaturedAssetUM>) : RwaFeaturedUM

    /** Nothing to show — a failed fetch or a backend without the `featured_assets` category. */
    data object Empty : RwaFeaturedUM
}

@Immutable
internal data class RwaFeaturedAssetUM(
    val id: String,
    val iconUrl: String,
    val name: TextReference,
    val ticker: TextReference,
    val price: TextReference,
    val priceChange: TangemPriceChange.State,
    val onClick: () -> Unit,
)

@Immutable
internal data class RwaTagUM(
    val id: String,
    val title: TextReference,
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