package com.tangem.features.feed.model.news.details.converter

import androidx.compose.runtime.Stable
import com.tangem.core.ui.components.label.entity.LabelLeadingContentUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.WrappedList
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.FormattedDate
import com.tangem.core.ui.utils.getFormattedDate
import com.tangem.data.common.currency.getTokenIconUrlFromDefaultHost
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.news.DetailedArticle
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.Source
import com.tangem.features.feed.ui.news.details.state.SourceUM
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.joda.time.DateTime

@Stable
internal class NewsDetailsConverter(
    private val onSourceClick: (String) -> Unit,
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
            sources = buildSources(value),
            newsUrl = value.newsUrl,
            relatedTokens = value.relatedTokens.toImmutableList(),
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

    private fun buildSources(article: DetailedArticle): ImmutableList<SourceUM> {
        return article.originalArticles.map { originalArticle ->
            SourceUM(
                id = originalArticle.id,
                title = originalArticle.title,
                source = Source(id = originalArticle.source.id, name = originalArticle.source.name),
                publishedAt = mapFormattedDate(originalArticle.publishedAt),
                url = originalArticle.url,
                onClick = { onSourceClick(originalArticle.url) },
                imageUrl = originalArticle.imageUrl,
            )
        }.toImmutableList()
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