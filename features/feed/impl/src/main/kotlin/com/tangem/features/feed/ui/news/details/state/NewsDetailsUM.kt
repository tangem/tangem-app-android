package com.tangem.features.feed.ui.news.details.state

import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class NewsDetailsUM(
    val articles: ImmutableList<ArticleUM>,
    val selectedArticleIndex: Int,
    val onShareClick: () -> Unit,
    val onLikeClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onArticleIndexChanged: (Int) -> Unit = {},
)

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
)

internal data class SourceUM(
    val id: Int,
    val title: String,
    val source: Source,
    val publishedAt: TextReference,
    val url: String,
    val onClick: () -> Unit,
)

internal data class Source(
    val id: Int,
    val name: String,
)