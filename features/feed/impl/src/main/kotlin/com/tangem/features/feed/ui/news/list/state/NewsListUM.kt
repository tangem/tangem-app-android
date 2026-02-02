package com.tangem.features.feed.ui.news.list.state

import androidx.compose.runtime.Immutable
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.core.ui.components.chip.entity.ChipUM
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class NewsListUM(
    val selectedCategoryId: Int,
    val filters: ImmutableList<ChipUM>,
    val listOfArticles: ImmutableList<ArticleConfigUM>,
    val newsListState: NewsListState,
    val onArticleClick: (Int) -> Unit,
    val onBackClick: () -> Unit,
)

@Immutable
sealed class NewsListState {
    data class Content(val loadMore: () -> Unit) : NewsListState()
    data object Loading : NewsListState()
    data class LoadingError(val onRetryClicked: () -> Unit) : NewsListState()
}