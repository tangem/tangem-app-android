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

    fun updateTrendingNewsState(news: List<ShortArticle>) {
        val trendingArticleIndex = news.indexOfFirst { it.isTrending }
        val trendingArticle = if (trendingArticleIndex != -1) news[trendingArticleIndex] else null
        val commonArticles = if (trendingArticleIndex != -1) {
            news.toMutableList().apply { removeAt(trendingArticleIndex) }
        } else {
            news
        }
        val currentState = currentStateProvider()
        onStateUpdate(
            currentState.copy(
                trendingArticle = trendingArticle?.let { article ->
                    ArticleConfigUM(
                        id = article.id,
                        title = article.title,
                        score = article.score,
                        isTrending = true,
                        tags = article.categories.map { category ->
                            LabelUM(text = TextReference.Str(category.name))
                        }.plus(
                            article.relatedTokens.map { token ->
                                LabelUM(
                                    text = TextReference.Str(token.symbol),
                                    leadingContent = LabelLeadingContentUM.Token(
                                        iconUrl = getTokenIconUrlFromDefaultHost(
                                            tokenId = CryptoCurrency.RawID(token.id),
                                        ),
                                    ),
                                )
                            },
                        ).toPersistentSet(),
                        createdAt = mapFormattedDate(article.createdAt),
                        isViewed = article.viewed,
                    )
                },
                news = NewsUM.Content(
                    commonArticles.map { article ->
                        ArticleConfigUM(
                            id = article.id,
                            title = article.title,
                            score = article.score,
                            isTrending = false,
                            tags = article.categories.map { category ->
                                LabelUM(text = TextReference.Str(category.name))
                            }.plus(
                                article.relatedTokens.map { token ->
                                    LabelUM(
                                        text = TextReference.Str(token.symbol),
                                        leadingContent = LabelLeadingContentUM.Token(
                                            iconUrl = getTokenIconUrlFromDefaultHost(
                                                tokenId = CryptoCurrency.RawID(token.id),
                                            ),
                                        ),
                                    )
                                },
                            ).toPersistentSet(),
                            createdAt = mapFormattedDate(article.createdAt),
                            isViewed = article.viewed,
                        )
                    }.toPersistentList(),
                ),
            ),
        )
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