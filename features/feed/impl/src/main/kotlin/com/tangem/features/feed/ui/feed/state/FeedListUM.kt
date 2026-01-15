package com.tangem.features.feed.ui.feed.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf

internal data class FeedListUM(
    val currentDate: String,
    val feedListSearchBar: FeedListSearchBar,
    val feedListCallbacks: FeedListCallbacks,
    val news: NewsUM,
    val trendingArticle: ArticleConfigUM?,
    val marketChartConfig: MarketChartConfig,
    val globalState: GlobalFeedState = GlobalFeedState.Content,
)

internal data class FeedListCallbacks(
    val onSearchClick: () -> Unit,
    val onMarketOpenClick: (sortBy: SortByTypeUM) -> Unit,
    val onArticleClick: (id: Int) -> Unit,
    val onOpenAllNews: (isFromCarouselButton: Boolean) -> Unit,
    val onMarketItemClick: (MarketsListItemUM) -> Unit,
    val onSortTypeClick: (sortBy: SortByTypeUM) -> Unit,
    val onSliderScroll: () -> Unit,
    val onSliderEndReached: () -> Unit,
)

internal data class FeedListSearchBar(
    val onBarClick: () -> Unit,
    val placeholderText: TextReference,
)

internal data class NewsUM(
    val content: ImmutableList<ArticleConfigUM>,
    val onRetryClicked: () -> Unit,
    val newsUMState: NewsUMState,
)

internal enum class NewsUMState {
    LOADING,
    CONTENT,
    ERROR,
}

internal data class MarketChartConfig(
    val marketCharts: ImmutableMap<SortByTypeUM, MarketChartUM>,
    val currentSortByType: SortByTypeUM = SortByTypeUM.TopGainers,
) {
    fun getFilterPreset() = persistentListOf(
        SortByTypeUM.Trending,
        SortByTypeUM.ExperiencedBuyers,
        SortByTypeUM.TopGainers,
        SortByTypeUM.TopLosers,
    )
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

@Immutable
internal sealed interface GlobalFeedState {
    data object Loading : GlobalFeedState
    data object Content : GlobalFeedState
    data class Error(val onRetryClicked: () -> Unit) : GlobalFeedState
}