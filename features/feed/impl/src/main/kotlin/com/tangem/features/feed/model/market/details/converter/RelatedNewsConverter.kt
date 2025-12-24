package com.tangem.features.feed.model.market.details.converter

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
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import org.joda.time.DateTime

class RelatedNewsConverter : Converter<List<ShortArticle>, ImmutableList<ArticleConfigUM>> {

    override fun convert(value: List<ShortArticle>): ImmutableList<ArticleConfigUM> {
        return value.map { shortArticle ->
            ArticleConfigUM(
                id = shortArticle.id,
                title = shortArticle.title,
                score = shortArticle.score,
                isTrending = false,
                tags = buildArticleTags(shortArticle),
                createdAt = mapFormattedDate(shortArticle.createdAt),
                isViewed = shortArticle.viewed,
            )
        }.toPersistentList()
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