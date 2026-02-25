package com.tangem.features.feed.ui.feed.state

import androidx.compose.runtime.Immutable
import com.tangem.features.feed.ui.feed.components.articles.ArticleConfigUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class NewsSliderConfig(
    val shouldShowSeeAllNewsItem: Boolean,
    val content: ImmutableList<ArticleConfigUM>,
    val callbacks: NewsSliderCallbacks,
)

@Immutable
internal data class NewsSliderCallbacks(
    val onOpenAllNews: () -> Unit,
    val onSliderScroll: () -> Unit,
    val onSliderEndReached: () -> Unit,
    val onArticleClick: (id: Int) -> Unit,
)