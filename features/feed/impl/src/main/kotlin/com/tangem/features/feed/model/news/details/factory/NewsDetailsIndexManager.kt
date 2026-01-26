package com.tangem.features.feed.model.news.details.factory

import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.utils.extensions.indexOfFirstOrNull
import kotlinx.collections.immutable.ImmutableList

internal object NewsDetailsIndexManager {

    /**
     * Calculate new index of article, trying to save current selected article while is loading
     */
    fun calculateNewIndex(
        currentState: NewsDetailsState,
        newArticles: ImmutableList<ArticleUM>,
        defaultArticleId: Int,
    ): Int {
        val currentArticleId = if (currentState.articles.isEmpty()) {
            defaultArticleId
        } else {
            currentState.articles.getOrNull(currentState.selectedArticleIndex)?.id
                ?: defaultArticleId
        }
        return newArticles.indexOfFirstOrNull { it.id == currentArticleId } ?: 0
    }

    data class NewsDetailsState(
        val articles: ImmutableList<ArticleUM>,
        val selectedArticleIndex: Int,
    )
}