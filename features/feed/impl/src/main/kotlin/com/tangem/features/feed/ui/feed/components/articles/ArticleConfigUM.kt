package com.tangem.features.feed.ui.feed.components.articles

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableSet

@Immutable
data class ArticleConfigUM(
    val id: Int,
    val title: String,
    val score: Float,
    val createdAt: TextReference,
    val isTrending: Boolean,
    val tags: ImmutableSet<LabelUM>,
    val isViewed: Boolean,
)