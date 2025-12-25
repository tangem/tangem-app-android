package com.tangem.features.news.list.impl.ui

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.chip.entity.ChipUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class NewsListUM(
    val selectedCategoryId: Int?,
    val filters: ImmutableList<ChipUM>,
    val articles: ImmutableList<ArticleConfigUM>,
    val onArticleClick: (Int) -> Unit,
)