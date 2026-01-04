package com.tangem.features.feed.ui.news.details.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.news.RelatedToken
import kotlinx.collections.immutable.ImmutableList

internal data class NewsDetailsUM(
    val articlesStateUM: ArticlesStateUM,
    val articles: ImmutableList<ArticleUM>,
    val selectedArticleIndex: Int,
    val onShareClick: () -> Unit,
    val onLikeClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onArticleIndexChanged: (Int) -> Unit,
    val relatedTokensUM: RelatedTokensUM = RelatedTokensUM.Loading,
)

@Immutable
internal sealed interface ArticlesStateUM {
    data object Loading : ArticlesStateUM
    data object Content : ArticlesStateUM
    data class LoadingError(val onRetryClicked: () -> Unit) : ArticlesStateUM
}

internal data class ArticleUM(
    val id: Int,
    val title: String,
    val createdAt: TextReference,
    val score: Float,
    val tags: ImmutableList<LabelUM>,
    val shortContent: String,
    val content: String,
    val sources: ImmutableList<SourceUM>,
    val newsUrl: String,
    val relatedTokens: ImmutableList<RelatedToken>,
)

internal data class SourceUM(
    val id: Int,
    val title: String,
    val source: Source,
    val publishedAt: TextReference,
    val url: String,
    val onClick: () -> Unit,
    val imageUrl: String?,
)

@Immutable
internal sealed interface RelatedTokensUM {

    data class Content(
        val items: ImmutableList<MarketsListItemUM>,
        val onTokenClick: (MarketsListItemUM) -> Unit,
    ) : RelatedTokensUM

    data object Loading : RelatedTokensUM

    data object LoadingError : RelatedTokensUM
}

internal data class Source(
    val id: Int,
    val name: String,
)