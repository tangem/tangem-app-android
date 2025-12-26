package com.tangem.features.feed.ui.feed.state

import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.models.news.TrendingNews
import com.tangem.features.feed.ui.utils.mapFormattedDate
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

internal class TrendingNewsStateFactory(
    private val currentStateProvider: Provider<FeedListUM>,
    private val onStateUpdate: (FeedListUM) -> Unit,
    private val onRetryClicked: () -> Unit,
) {

    fun updateTrendingNewsState(result: TrendingNews) {
        val currentState = currentStateProvider()
        when (result) {
            is TrendingNews.Data -> handleDataState(currentState, result.articles)
            is TrendingNews.Error -> handleErrorState(currentState)
        }
    }

    private fun handleDataState(currentState: FeedListUM, articles: List<ShortArticle>) {
        val (trendingArticle, commonArticles) = separateTrendingAndCommonArticles(articles)
        val commonArticlesUM = commonArticles.map { mapToArticleConfigUM(it, isTrending = false) }.toPersistentList()
        val updatedNews = when (currentState.news.newsUMState) {
            NewsUMState.CONTENT -> currentState.news.copy(content = commonArticlesUM)
            NewsUMState.LOADING,
            NewsUMState.ERROR,
            -> NewsUM(
                content = commonArticlesUM,
                onRetryClicked = onRetryClicked,
                newsUMState = NewsUMState.CONTENT,
            )
        }

        onStateUpdate(
            currentState.copy(
                trendingArticle = trendingArticle?.let { mapToArticleConfigUM(it, isTrending = true) },
                news = updatedNews,
            ),
        )
    }

    private fun handleErrorState(currentState: FeedListUM) {
        onStateUpdate(
            currentState.copy(
                trendingArticle = null,
                news = NewsUM(
                    content = persistentListOf(),
                    onRetryClicked = onRetryClicked,
                    newsUMState = NewsUMState.ERROR,
                ),
            ),
        )
    }

    private fun separateTrendingAndCommonArticles(
        articles: List<ShortArticle>,
    ): Pair<ShortArticle?, List<ShortArticle>> {
        val trendingArticleIndex = articles.indexOfFirst { it.isTrending }
        return if (trendingArticleIndex != -1) {
            val trendingArticle = articles[trendingArticleIndex]
            val commonArticles = articles.toMutableList().apply { removeAt(trendingArticleIndex) }
            trendingArticle to commonArticles
        } else {
            null to articles
        }
    }

    private fun mapToArticleConfigUM(article: ShortArticle, isTrending: Boolean): ArticleConfigUM {
        return ArticleConfigUM(
            id = article.id,
            title = article.title,
            score = article.score,
            isTrending = isTrending,
            tags = buildArticleTags(article),
            createdAt = mapFormattedDate(article.createdAt),
            isViewed = article.viewed,
        )
    }

    private fun buildArticleTags(article: ShortArticle): ImmutableSet<LabelUM> {
        val categoryLabels = article.categories.map { category ->
            LabelUM(text = TextReference.Str(category.name))
        }
        val tokenLabels = article.relatedTokens.map { token ->
            LabelUM(
                text = TextReference.Str(token.symbol),
                leadingContent = LabelLeadingContentUM.Token(
                    iconUrl = getTokenIconUrlFromDefaultHost(
                        tokenId = CryptoCurrency.RawID(token.id),
                    ),
                ),
            )
        }
        return (categoryLabels + tokenLabels).toPersistentSet()
    }
}