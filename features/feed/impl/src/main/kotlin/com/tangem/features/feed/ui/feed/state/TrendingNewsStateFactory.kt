package com.tangem.features.feed.ui.feed.state

import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.FormattedDate
import com.tangem.core.ui.utils.getFormattedDate
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.ShortArticle
import com.tangem.domain.models.news.TrendingNews
import com.tangem.features.feed.impl.R
import com.tangem.utils.Provider
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import org.joda.time.DateTime

internal class TrendingNewsStateFactory(
    private val currentStateProvider: Provider<FeedListUM>,
    private val onStateUpdate: (FeedListUM) -> Unit,
) {

    fun updateTrendingNewsState(result: TrendingNews, onRetryClicked: () -> Unit) {
        val currentState = currentStateProvider()
        when (result) {
            is TrendingNews.Data -> handleDataState(currentState, result.articles)
            is TrendingNews.Error -> handleErrorState(currentState, onRetryClicked)
        }
    }

    private fun handleDataState(currentState: FeedListUM, articles: List<ShortArticle>) {
        val (trendingArticle, commonArticles) = separateTrendingAndCommonArticles(articles)

        onStateUpdate(
            currentState.copy(
                trendingArticle = trendingArticle?.let { mapToArticleConfigUM(it, isTrending = true) },
                news = NewsUM.Content(
                    commonArticles.map { mapToArticleConfigUM(it, isTrending = false) }.toPersistentList(),
                ),
            ),
        )
    }

    private fun handleErrorState(currentState: FeedListUM, onRetryClicked: () -> Unit) {
        onStateUpdate(
            currentState.copy(
                trendingArticle = null,
                news = NewsUM.Error(onRetryClicked = onRetryClicked),
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

    private fun buildArticleTags(article: ShortArticle): kotlinx.collections.immutable.ImmutableSet<LabelUM> {
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

    private fun mapFormattedDate(createdAt: String): TextReference {
        val formattedDate = getFormattedDate(
            createdAt = createdAt,
            now = DateTime.now(),
        )
        return when (formattedDate) {
            is FormattedDate.FullDate -> TextReference.Str(value = formattedDate.date)
            is FormattedDate.HoursAgo -> TextReference.PluralRes(
                id = R.plurals.news_published_hours_ago,
                count = formattedDate.hours,
                formatArgs = wrappedList(formattedDate.hours),
            )
            is FormattedDate.MinutesAgo -> TextReference.PluralRes(
                id = R.plurals.news_published_minutes_ago,
                count = formattedDate.minutes,
                formatArgs = wrappedList(formattedDate.minutes),
            )
            is FormattedDate.Today -> TextReference.Combined(
                refs = WrappedList(
                    data = listOf(
                        TextReference.Res(R.string.common_today),
                        TextReference.Str(StringsSigns.COMA_SIGN),
                        TextReference.Str(StringsSigns.WHITE_SPACE),
                        TextReference.Str(formattedDate.time),
                    ),
                ),
            )
        }
    }
}