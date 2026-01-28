package com.tangem.features.news.details.impl.ui

import com.tangem.core.ui.components.label.entity.LabelUM
import kotlinx.collections.immutable.ImmutableList

// TODO [REDACTED_TASK_KEY] make internal
data class NewsDetailsUM(
    val articles: ImmutableList<ArticleUM>,
    val selectedArticleIndex: Int,
    val onShareClick: () -> Unit,
    val onLikeClick: () -> Unit,
)

// TODO [REDACTED_TASK_KEY] make internal
data class ArticleUM(
    val id: Int,
    val title: String,
    val createdAt: String,
    val score: Float,
    val tags: ImmutableList<LabelUM>,
    val shortContent: String,
    val content: String,
    val sources: ImmutableList<SourceUM>,
)

// TODO [REDACTED_TASK_KEY] make internal
data class SourceUM(
    val id: Int,
    val title: String,
    val sourceName: String,
    val publishedAt: String,
    val url: String,
)