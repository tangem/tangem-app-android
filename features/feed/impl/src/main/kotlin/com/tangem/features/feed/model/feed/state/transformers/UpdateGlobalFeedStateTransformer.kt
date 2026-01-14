package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.api.common.response.ApiResponseError
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
        val hasErrorInCharts = errorStatesByOrder.values.any { it != null }
        val hasAllErrorInCharts = errorStatesByOrder.values.isNotEmpty() && errorStatesByOrder.values.all { it != null }

        val areAllChartsLoading = loadingStatesByOrder.values.isNotEmpty() && loadingStatesByOrder.values.all { it }
        val areAllChartsError = hasAllErrorInCharts || hasErrorInCharts && hasLoadingInCharts

        val newGlobalState = when {
            isNewsLoading && areAllChartsError -> GlobalFeedState.Loading
            isNewsLoading && areAllChartsLoading -> GlobalFeedState.Loading
            isNewsError && areAllChartsLoading -> GlobalFeedState.Loading
            isNewsError && areAllChartsError -> {
                if (previousGlobalState !is GlobalFeedState.Error) {
                    sendErrorAnalytics(errorStatesByOrder)
                }
                GlobalFeedState.Error(
                    onRetryClicked = onRetryClicked,
                )
            }
            else -> GlobalFeedState.Content
        }

        return prevState.copy(
            globalState = newGlobalState,
            news = prevState.news.copy(
                newsUMState = when {
                    isNewsError -> NewsUMState.ERROR
                    else -> NewsUMState.CONTENT
                },
            ),
        )
    }

    private fun sendErrorAnalytics(errorStatesByOrder: Map<SortByTypeUM, Throwable?>) {
        val firstError: Throwable? = errorStatesByOrder.values.firstNotNullOfOrNull { it }
        firstError?.let { error ->
            val (code, message) = when (error) {
                is ApiResponseError.HttpException -> error.code.numericCode to error.message
                else -> null to ""
            }
            analyticsEventHandler.send(
                FeedAnalyticsEvent.AllWidgetsLoadError(
                    code = code,
                    message = message.orEmpty(),
                ),
            )
        }
    }
}