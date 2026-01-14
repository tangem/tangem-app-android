package com.tangem.features.feed.model.feed.state.transformers

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.news.NewsError
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.models.news.TrendingNews
import com.tangem.features.feed.model.converter.ShortArticleToArticleConfigUMConverter
import com.tangem.features.feed.model.feed.analytics.FeedAnalyticsEvent
import com.tangem.features.feed.ui.feed.state.FeedListUM
import com.tangem.features.feed.ui.feed.state.NewsUM
import com.tangem.features.feed.ui.feed.state.NewsUMState
import com.tangem.utils.Provider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class UpdateTrendingNewsStateTransformer(
    private val result: TrendingNews,
    private val onRetryClicked: () -> Unit,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : FeedListUMTransformer {

    override fun transform(prevState: FeedListUM): FeedListUM {
        return when (result) {
            is TrendingNews.Data -> handleDataState(prevState, result.articles)
            is TrendingNews.Error -> {
                val (code, message) = when (val error = result.error) {
                    is NewsError.HttpError -> error.code to error.message
                    NewsError.NotHttpError -> null to ""
                }
                analyticsEventHandler.send(
                    FeedAnalyticsEvent.NewsLoadError(
                        code = code,
                        message = message,
                    ),
                )
                handleErrorState(prevState)
            }
        }
    }

    private fun handleDataState(currentState: FeedListUM, articles: List<ShortArticle>): FeedListUM {
        val (trendingArticle, commonArticles) = separateTrendingAndCommonArticles(articles)
        val commonArticlesUM = getShortArticleConfigConverter(isTrending = false)
            .convert(commonArticles)
            .toPersistentList()
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

        return currentState.copy(
            trendingArticle = trendingArticle?.let { article ->
                getShortArticleConfigConverter(isTrending = true)
                    .convert(listOf(article))
            }?.firstOrNull(),
            news = updatedNews,
        )
    }

    private fun handleErrorState(currentState: FeedListUM): FeedListUM {
        return currentState.copy(
            trendingArticle = null,
            news = NewsUM(
                content = persistentListOf(),
                onRetryClicked = onRetryClicked,
                newsUMState = NewsUMState.ERROR,
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

    private fun getShortArticleConfigConverter(isTrending: Boolean): ShortArticleToArticleConfigUMConverter {
        return ShortArticleToArticleConfigUMConverter(isTrending = Provider { isTrending })
    }
}