package com.tangem.features.feed.ui.news.list.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.news.ArticleCard
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.common.ui.news.DefaultLoadingArticle
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.list.InfiniteListHandler
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.news.list.state.NewsListState
import kotlinx.collections.immutable.ImmutableList

private const val LOAD_NEXT_PAGE_ON_END_INDEX = 40

@Composable
internal fun NewsListLazyColumn(
    listOfArticles: ImmutableList<ArticleConfigUM>,
    newsListState: NewsListState,
    lazyListState: LazyListState,
    onArticleClick: (Int) -> Unit,
) {
    val screenState by remember(listOfArticles, newsListState) {
        derivedStateOf {
            val isListEmpty = listOfArticles.isEmpty()
            when {
                isListEmpty && newsListState is NewsListState.Loading -> NewsListScreenState.InitialLoading
                isListEmpty && newsListState is NewsListState.LoadingError -> NewsListScreenState.InitialError
                else -> NewsListScreenState.Content
            }
        }
    }

    AnimatedContent(
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        targetState = screenState,
        label = "NewsListTransition",
    ) { state ->
        when (state) {
            NewsListScreenState.Content -> {
                Content(
                    listOfArticles = listOfArticles,
                    newsListState = newsListState,
                    lazyListState = lazyListState,
                    onArticleClick = onArticleClick,
                )
            }
            NewsListScreenState.InitialLoading -> {
                LazyColumn(
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    userScrollEnabled = false,
                ) {
                    items(
                        count = 10,
                        key = { "initial_loading_$it" },
                    ) {
                        DefaultLoadingArticle(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(164.dp),
                        )
                        SpacerH(12.dp)
                    }
                }
            }
            NewsListScreenState.InitialError -> {
                val errorState = newsListState as? NewsListState.LoadingError
                LoadingErrorItem(
                    modifier = Modifier.fillMaxSize(),
                    onTryAgain = errorState?.onRetryClicked ?: {},
                )
            }
        }
    }
}

@Composable
private fun Content(
    listOfArticles: ImmutableList<ArticleConfigUM>,
    newsListState: NewsListState,
    lazyListState: LazyListState,
    onArticleClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 16.dp, start = 16.dp, end = 16.dp),
        userScrollEnabled = true,
    ) {
        items(
            items = listOfArticles,
            key = ArticleConfigUM::id,
        ) { article ->
            ArticleCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp),
                colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
                articleConfigUM = article,
                onArticleClick = {
                    onArticleClick(article.id)
                },
            )
            SpacerH(12.dp)
        }

        if (newsListState is NewsListState.Loading) {
            items(
                count = 10,
                key = { "loading_footer_$it" },
            ) {
                DefaultLoadingArticle(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp),
                )
                SpacerH(12.dp)
            }
        }
    }

    if (newsListState is NewsListState.Content) {
        InfiniteListHandler(
            listState = lazyListState,
            buffer = LOAD_NEXT_PAGE_ON_END_INDEX,
            triggerLoadMoreCheckOnItemsCountChange = true,
            onLoadMore = remember(newsListState) {
                {
                    newsListState.loadMore()
                    true
                }
            },
        )
    }
}

private enum class NewsListScreenState {
    InitialLoading,
    InitialError,
    Content,
}

@Composable
private fun LoadingErrorItem(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(vertical = 35.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        UnableToLoadData(onRetryClick = onTryAgain)
    }
}