package com.tangem.features.feed.model.converter

import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.ShortArticle
import com.tangem.features.feed.ui.utils.mapFormattedDate
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

internal class ShortArticleToArticleConfigUMConverter(
    private val isTrending: Provider<Boolean>,
) : Converter<List<ShortArticle>, ImmutableList<ArticleConfigUM>> {

    override fun convert(value: List<ShortArticle>): ImmutableList<ArticleConfigUM> {
        return value.map { shortArticle ->
            ArticleConfigUM(
                id = shortArticle.id,
                title = shortArticle.title,
                score = shortArticle.score,
                isTrending = isTrending(),
                tags = buildArticleTags(shortArticle),
                createdAt = mapFormattedDate(shortArticle.createdAt),
                isViewed = shortArticle.viewed,
            )
        }.toPersistentList()
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