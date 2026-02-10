package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.news.TrendingNews
import com.tangem.features.feed.entry.featuretoggle.FeedFeatureToggle
import com.tangem.features.feed.model.feed.analytics.FeedAnalyticsEvent
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.earn.state.EarnListUM
import com.tangem.features.feed.ui.feed.state.FeedListUM
import com.tangem.features.feed.ui.feed.state.GlobalFeedState
import com.tangem.features.feed.ui.feed.state.NewsUMState

@Suppress("LongParameterList")
internal class UpdateGlobalFeedStateTransformer(
    private val loadingStatesByOrder: Map<SortByTypeUM, Boolean>,
    private val errorStatesByOrder: Map<SortByTypeUM, Throwable?>,
    private val trendingNewsResult: TrendingNews,
    private val earnResult: EarnTopToken?,
    private val onRetryClicked: () -> Unit,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val feedFeatureToggle: FeedFeatureToggle,
) : FeedListUMTransformer {

    override fun transform(prevState: FeedListUM): FeedListUM {
        val blockStates = buildList {
            add(getNewsState(prevState))
            add(getChartsState())
            if (feedFeatureToggle.isEarnBlockEnabled) {
                add(getEarnState(prevState))
            }
        }

        val newGlobalState = when {
            blockStates.all { it == BlockState.ERROR } -> {
                if (prevState.globalState !is GlobalFeedState.Error) {
                    sendErrorAnalytics()
                }
                GlobalFeedState.Error(onRetryClicked = onRetryClicked)
            }
            blockStates.any { it == BlockState.LOADING } -> GlobalFeedState.Loading
            else -> GlobalFeedState.Content
        }

        return prevState.copy(globalState = newGlobalState)
    }

    private fun areAllChartsError(): Boolean {
        val hasLoadingInCharts = loadingStatesByOrder.values.any { it }
        val hasErrorInMarketChart = errorStatesByOrder[SortByTypeUM.Rating] != null
        val hasErrorInMarketPulseChart = errorStatesByOrder
            .any { it.key != SortByTypeUM.Rating && it.value != null }
        return hasErrorInMarketChart && hasErrorInMarketPulseChart ||
            hasErrorInMarketPulseChart && hasLoadingInCharts ||
            hasErrorInMarketChart && hasLoadingInCharts
    }

    private fun sendErrorAnalytics() {
        analyticsEventHandler.send(
            FeedAnalyticsEvent.AllWidgetsLoadError(),
        )
    }

    private fun getNewsState(prevState: FeedListUM): BlockState {
        val isError = trendingNewsResult is TrendingNews.Error || prevState.news.newsUMState == NewsUMState.ERROR
        if (isError) return BlockState.ERROR

        val isLoading = prevState.news.newsUMState == NewsUMState.LOADING && trendingNewsResult !is TrendingNews.Data
        if (isLoading) return BlockState.LOADING

        return BlockState.CONTENT
    }

    private fun getChartsState(): BlockState {
        if (areAllChartsError()) return BlockState.ERROR

        val isLoading = loadingStatesByOrder.values.isNotEmpty() && loadingStatesByOrder.values.all { it }
        if (isLoading) return BlockState.LOADING

        return BlockState.CONTENT
    }

    private fun getEarnState(prevState: FeedListUM): BlockState {
        val isError = when (earnResult) {
            null -> false
            else -> earnResult.isLeft()
        }
        if (isError) return BlockState.ERROR

        val isLoading = prevState.earnListUM is EarnListUM.Loading && earnResult == null
        if (isLoading) return BlockState.LOADING

        return BlockState.CONTENT
    }

    private enum class BlockState { LOADING, ERROR, CONTENT }
}