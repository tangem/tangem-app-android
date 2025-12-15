package com.tangem.features.feed.ui.feed.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.features.feed.ui.market.state.SortByTypeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentList

internal data class FeedListUM(
    val currentDate: String,
    val searchBar: SearchBarUM,
    val feedListCallbacks: FeedListCallbacks,
    val news: NewsUM,
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

@Immutable
internal sealed interface NewsUM {
    data object Loading : NewsUM
    data class Content(val content: ImmutableList<ArticleConfigUM>) : NewsUM
}

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
        val sortChartConfig: SortChartConfigUM,
    ) : MarketChartUM

    data object Loading : MarketChartUM

    data class LoadingError(val onRetryClicked: () -> Unit) : MarketChartUM
}

internal data class SortChartConfigUM(
    val sortByType: SortByTypeUM,
    val isSelected: Boolean,
)