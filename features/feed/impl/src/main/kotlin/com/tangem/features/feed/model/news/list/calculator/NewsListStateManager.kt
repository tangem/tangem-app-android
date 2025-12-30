package com.tangem.features.feed.model.news.list.calculator

import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.features.feed.ui.news.list.state.NewsListState
import com.tangem.pagination.PaginationStatus
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * A pure function for calculating the state of the news list.
 * Extracted from NewsListModel to simplify testing and improve readability.
 */
internal object NewsListStateManager {

    /**
     * Calculates the state of the news list based on the current data.
     *
     * @param articles the current list of articles
     * @param isError flag indicating an initial loading error
     * @param paginationStatus the current pagination status
     * @param onRetryClick handler for the retry loading action
     * @param onLoadMore handler for loading the next page
     * @return a pair: the list state and the list of articles to display
     */
    fun calculateState(
        articles: ImmutableList<ArticleConfigUM>,
        isError: Boolean,
        paginationStatus: PaginationStatus<*>,
        onRetryClick: () -> Unit,
        onLoadMore: () -> Unit,
    ): Pair<NewsListState, ImmutableList<ArticleConfigUM>> {
        return when {
            isError -> NewsListState.LoadingError(onRetryClick) to persistentListOf()
            paginationStatus is PaginationStatus.InitialLoading && articles.isEmpty() -> {
                NewsListState.Loading to persistentListOf()
            }
            articles.isEmpty() -> NewsListState.Loading to persistentListOf()
            else -> NewsListState.Content(loadMore = onLoadMore) to articles
        }
    }
}