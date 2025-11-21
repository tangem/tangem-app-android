package com.tangem.features.feed.ui.feed.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.StateEvent
import com.tangem.features.feed.ui.market.state.MarketsListItemUM
import com.tangem.features.feed.ui.market.state.SortByTypeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentList

internal data class FeedListUM(
    val currentDate: String,
    val searchBar: SearchBarUM,
    val feedListCallbacks: FeedListCallbacks,
    val news: ImmutableList<ArticleConfigUM>,
    val trendingArticle: ArticleConfigUM?,
    val marketChartConfig: MarketChartConfig,
)

internal data class FeedListCallbacks(
    val onSearchClick: () -> Unit,
    val onMarketOpenClick: (sortBy: SortByTypeUM) -> Unit,
    val onArticleClick: (id: Int) -> Unit,
    val onOpenAllNews: () -> Unit,
    val onMarketItemClick: (MarketsListItemUM) -> Unit,
    val onSortTypeClick: (SortByTypeUM) -> Unit,
)

internal data class MarketChartConfig(
    val marketCharts: ImmutableMap<SortByTypeUM, MarketChartUM>,
    val currentSortByType: SortByTypeUM = SortByTypeUM.TopGainers,
) {
    fun getFilterPreset() = SortByTypeUM.entries.filter { it != SortByTypeUM.Rating }.toPersistentList()
}

@Immutable
internal sealed interface MarketChartUM {

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
        val triggerScrollReset: StateEvent<Unit>,
        val sortChartConfig: SortChartConfigUM,
    ) : MarketChartUM

    data object Loading : MarketChartUM

    data class LoadingError(val onRetryClicked: () -> Unit) : MarketChartUM
}

data class SortChartConfigUM(
    val sortByType: SortByTypeUM,
    val isSelected: Boolean,
)