package com.tangem.features.feed.model.news.details.factory

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.navigation.share.ShareManager
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.NewsDetailsUM
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toImmutableList

internal class NewsDetailsStateFactory(
    private val currentStateProvider: Provider<NewsDetailsUM>,
    private val shareManager: ShareManager,
    private val onStateUpdate: (NewsDetailsUM) -> Unit,
) {

    fun updateArticles(articles: List<ArticleUM>, selectedIndex: Int) {
        val currentState = currentStateProvider()
        val currentArticle = articles.getOrNull(selectedIndex)
        onStateUpdate(
            currentState.copy(
                articles = articles.toImmutableList(),
                selectedArticleIndex = selectedIndex,
                onShareClick = {
                    currentArticle?.let {
                        shareManager.shareText(it.newsUrl)
                    }
                },
            ),
        )
    }

    fun updateSelectedArticleIndex(newIndex: Int) {
        val currentState = currentStateProvider()
        val currentArticle = currentState.articles.getOrNull(newIndex)
        onStateUpdate(
            currentState.copy(
                selectedArticleIndex = newIndex,
                onShareClick = {
                    currentArticle?.let {
                        shareManager.shareText(it.newsUrl)
                    }
                },
            ),
        )
    }

    fun updateRelatedTokens(relatedTokens: RelatedTokensUM) {
        val currentState = currentStateProvider()
        onStateUpdate(currentState.copy(relatedTokensUM = relatedTokens))
    }

    fun createRelatedTokensContent(
        items: List<MarketsListItemUM>,
        onTokenClick: (MarketsListItemUM) -> Unit,
    ): RelatedTokensUM.Content {
        return RelatedTokensUM.Content(
            items = items.toImmutableList(),
            onTokenClick = onTokenClick,
        )
    }
}