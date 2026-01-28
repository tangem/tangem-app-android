package com.tangem.features.feed.model.news.list.statemanager

import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.features.feed.model.news.list.analytics.NewsListAnalyticsEvent
import com.tangem.features.feed.ui.news.list.state.NewsListState
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class NewsListStateManager(
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    /**
     * Calculates the state of the news list based on the current data.
     *
     * @param articles the current list of articles
     * @param error flag indicating an initial loading error
     * @param paginationStatus the current pagination status
     * @param onRetryClick handler for the retry loading action
     * @param onLoadMore handler for loading the next page
     * @return a pair: the list state and the list of articles to display
     */
    fun calculateState(
        articles: ImmutableList<ArticleConfigUM>,
        error: Throwable?,
        paginationStatus: PaginationStatus<*>,
        onRetryClick: () -> Unit,
        onLoadMore: () -> Unit,
    ): Pair<NewsListState, ImmutableList<ArticleConfigUM>> {
        if (error != null) {
            sendErrorAnalytics(error)
        }
        return when {
            error != null -> NewsListState.LoadingError(onRetryClick) to persistentListOf()
            paginationStatus is PaginationStatus.InitialLoading && articles.isEmpty() -> {
                NewsListState.Loading to persistentListOf()
            }
            articles.isEmpty() -> NewsListState.Loading to persistentListOf()
            else -> NewsListState.Content(loadMore = onLoadMore) to articles
        }
    }

    private fun sendErrorAnalytics(throwable: Throwable) {
        val (code, message) = when (throwable) {
            is ApiResponseError.HttpException -> throwable.code.numericCode to throwable.message
            else -> null to ""
        }
        analyticsEventHandler.send(
            NewsListAnalyticsEvent.NewsListLoadError(
                code = code,
                message = message.orEmpty(),
            ),
        )
    }
}