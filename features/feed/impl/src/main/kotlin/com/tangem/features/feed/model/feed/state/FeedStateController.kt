package com.tangem.features.feed.model.feed.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.features.feed.model.feed.state.transformers.FeedListUMTransformer
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.feed.state.*
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class FeedStateController @Inject constructor() {

    private val mutableUiState: MutableStateFlow<FeedListUM> = MutableStateFlow(value = getInitialState())

    val uiState: StateFlow<FeedListUM> get() = mutableUiState.asStateFlow()

    val value: FeedListUM get() = uiState.value

    fun update(function: (FeedListUM) -> FeedListUM) {
        mutableUiState.update(function = function)
    }

    fun update(transformer: FeedListUMTransformer) {
        mutableUiState.update(function = transformer::transform)
    }

    fun updateAll(vararg transformers: FeedListUMTransformer) {
        transformers.forEach { mutableUiState.update(function = it::transform) }
    }

    fun clear() {
        mutableUiState.update { getInitialState() }
    }

    private fun getInitialState(): FeedListUM {
        return FeedListUM(
            currentDate = "",
            feedListSearchBar = FeedListSearchBar(
                placeholderText = com.tangem.core.ui.extensions.TextReference.EMPTY,
                onBarClick = {},
            ),
            feedListCallbacks = FeedListCallbacks(
                onSearchClick = {},
                onMarketOpenClick = {},
                onArticleClick = {},
                onOpenAllNews = {},
                onMarketItemClick = {},
                onSortTypeClick = {},
                onSliderScroll = {},
                onSliderEndReached = {},
            ),
            news = NewsUM(
                content = persistentListOf(),
                onRetryClicked = {},
                newsUMState = NewsUMState.LOADING,
            ),
            trendingArticle = null,
            marketChartConfig = MarketChartConfig(
                marketCharts = persistentHashMapOf(),
                currentSortByType = SortByTypeUM.Trending,
            ),
            globalState = GlobalFeedState.Loading,
        )
    }
}