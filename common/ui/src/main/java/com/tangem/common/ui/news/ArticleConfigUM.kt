package com.tangem.common.ui.news

import kotlinx.collections.immutable.ImmutableSet

data class ArticleConfigUM(
    val id: Int,
    val title: String,
    val score: Float,
    val createdAt: String,
    val isTrending: Boolean,
    val tags: ImmutableSet<ArticleTagUM>,
    val isViewed: Boolean,
)