package com.tangem.features.feed.model.news.details.factory

import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.ArticlesStateUM
import com.tangem.features.feed.ui.news.details.state.NewsDetailsUM
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toImmutableList

internal class NewsDetailsStateFactory(
    private val currentStateProvider: Provider<NewsDetailsUM>,
    private val onShareClick: (ArticleUM) -> Unit,
    private val onStateUpdate: (NewsDetailsUM) -> Unit,
    private val onRetryClick: () -> Unit,
) {

    fun updateArticles(articles: List<ArticleUM>, selectedIndex: Int) {
        val currentState = currentStateProvider()
        val currentArticle = articles.getOrNull(selectedIndex)
        onStateUpdate(
            currentState.copy(
                articles = articles.toImmutableList(),
                articlesStateUM = ArticlesStateUM.Content,
                selectedArticleIndex = selectedIndex,
                onShareClick = { currentArticle?.let(onShareClick) },
            ),
        )
    }

    fun updateSelectedArticleIndex(newIndex: Int) {
        val currentState = currentStateProvider()
        val currentArticle = currentState.articles.getOrNull(newIndex)
        onStateUpdate(
            currentState.copy(
                selectedArticleIndex = newIndex,
                onShareClick = { currentArticle?.let(onShareClick) },
            ),
        )
    }

    fun updateRelatedTokens(relatedTokens: RelatedTokensUM) {
        val currentState = currentStateProvider()
        onStateUpdate(currentState.copy(relatedTokensUM = relatedTokens))
    }

    fun createErrorState() {
        val currentState = currentStateProvider()
        onStateUpdate(currentState.copy(articlesStateUM = ArticlesStateUM.LoadingError(onRetryClick)))
    }

    fun createLoadingState() {
        val currentState = currentStateProvider()
        onStateUpdate(currentState.copy(articlesStateUM = ArticlesStateUM.Loading))
    }
}