package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.news.TrendingNews
import com.tangem.features.feed.model.feed.analytics.FeedAnalyticsEvent
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.feed.state.FeedListUM
import com.tangem.features.feed.ui.feed.state.GlobalFeedState
import com.tangem.features.feed.ui.feed.state.NewsUMState

internal class UpdateGlobalFeedStateTransformer(
    private val loadingStatesByOrder: Map<SortByTypeUM, Boolean>,
    private val errorStatesByOrder: Map<SortByTypeUM, Throwable?>,
    private val trendingNewsResult: TrendingNews,
    private val onRetryClicked: () -> Unit,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : FeedListUMTransformer {

    @Suppress("CyclomaticComplexMethod")
    override fun transform(prevState: FeedListUM): FeedListUM {
        val previousGlobalState = prevState.globalState

        val isNewsLoading = prevState.news.newsUMState == NewsUMState.LOADING
        val isNewsError = trendingNewsResult is TrendingNews.Error ||
            prevState.news.newsUMState == NewsUMState.ERROR

        val hasLoadingInCharts = loadingStatesByOrder.values.any { it }
        val hasErrorInMarketChart = errorStatesByOrder[SortByTypeUM.Rating] != null
        val hasErrorInMarketPulseChart = errorStatesByOrder
            .any { it.key != SortByTypeUM.Rating && it.value != null }

        val areAllChartsLoading = loadingStatesByOrder.values.isNotEmpty() && loadingStatesByOrder.values.all { it }
        val areAllChartsError = when {
            hasErrorInMarketChart && hasErrorInMarketPulseChart -> true
            hasErrorInMarketPulseChart && hasLoadingInCharts -> true
            hasErrorInMarketChart && hasLoadingInCharts -> true
            else -> false
        }

        val newGlobalState = when {
            isNewsError && areAllChartsLoading -> GlobalFeedState.Loading
            isNewsError && areAllChartsError -> {
                if (previousGlobalState !is GlobalFeedState.Error) {
                    sendErrorAnalytics()
                }
                GlobalFeedState.Error(
                    onRetryClicked = onRetryClicked,
                )
            }
            isNewsLoading && areAllChartsError -> GlobalFeedState.Loading
            isNewsLoading && areAllChartsLoading -> GlobalFeedState.Loading
            else -> GlobalFeedState.Content
        }
        return prevState.copy(globalState = newGlobalState)
    }

    private fun sendErrorAnalytics() {
        analyticsEventHandler.send(
            FeedAnalyticsEvent.AllWidgetsLoadError(),
        )
    }
}