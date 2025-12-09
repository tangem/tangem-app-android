package com.tangem.common.ui.news

import com.tangem.core.ui.components.label.entity.LabelUM
import kotlinx.collections.immutable.ImmutableSet

data class ArticleConfigUM(
    val id: Int,
    val title: String,
    val score: Float,
    val createdAt: String,
    val isTrending: Boolean,
    val tags: ImmutableSet<LabelUM>,
    val isViewed: Boolean,
)