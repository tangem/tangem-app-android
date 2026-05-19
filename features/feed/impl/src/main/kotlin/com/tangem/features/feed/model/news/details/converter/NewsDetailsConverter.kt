package com.tangem.features.feed.model.news.details.converter

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.utils.mapFormattedDate
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.DetailedArticle
import com.tangem.domain.models.news.RelatedArticle
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.Media
import com.tangem.features.feed.ui.news.details.state.RelatedArticleUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Stable
internal class NewsDetailsConverter(
    private val onRelatedArticleClick: (RelatedArticle) -> Unit,
) : Converter<DetailedArticle, ArticleUM> {

    override fun convert(value: DetailedArticle): ArticleUM {
        return ArticleUM(
            id = value.id,
            title = value.title,
            createdAt = mapFormattedDate(value.createdAt),
            score = value.score,
            tags = buildTags(value),
            shortContent = value.shortContent,
            content = value.content,
            relatedArticles = buildRelatedArticles(value),
            newsUrl = value.newsUrl,
            relatedTokens = value.relatedTokens.toImmutableList(),
            isLiked = value.isLiked,
            isTrending = value.isTrending,
        )
    }

    private fun buildTags(article: DetailedArticle): ImmutableList<LabelUM> {
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
        return (categoryLabels + tokenLabels).toImmutableList()
    }

    private fun buildRelatedArticles(article: DetailedArticle): ImmutableList<RelatedArticleUM> {
        return article.relatedArticles.map { relatedArticle ->
            RelatedArticleUM(
                id = relatedArticle.id,
                title = relatedArticle.title,
                media = Media(id = relatedArticle.media.id, name = relatedArticle.media.name),
                publishedAt = mapFormattedDate(relatedArticle.publishedAt),
                url = relatedArticle.url,
                onClick = { onRelatedArticleClick(relatedArticle) },
                imageUrl = relatedArticle.imageUrl,
            )
        }.toImmutableList()
    }
}