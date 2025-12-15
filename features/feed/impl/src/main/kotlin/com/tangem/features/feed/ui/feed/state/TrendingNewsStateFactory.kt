package com.tangem.features.feed.ui.feed.state

import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.ShortArticle
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

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
                        createdAt = "1 min ago", // TODO in [REDACTED_TASK_KEY]
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
                            createdAt = "1 min ago", // TODO in [REDACTED_TASK_KEY]
                            isViewed = article.viewed,
                        )
                    }.toPersistentList(),
                ),
            ),
        )
    }
}