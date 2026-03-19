package com.tangem.features.feed.ui.news.details

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.pager.PagerIndicator
import com.tangem.core.ui.ds.TangemPagerIndicator
import com.tangem.core.ui.ds.TangemPagerIndicatorColors
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.*
import com.tangem.features.feed.ui.news.details.components.ArticleDetail
import com.tangem.features.feed.ui.news.details.components.NewsDetailsPlaceholder
import com.tangem.features.feed.ui.news.details.state.ArticlesStateUM
import com.tangem.features.feed.ui.news.details.state.MockArticlesFactory
import com.tangem.features.feed.ui.news.details.state.NewsDetailsUM

@Composable
internal fun NewsDetailsContent(state: NewsDetailsUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    AnimatedContent(
        targetState = state.articlesStateUM,
        modifier = modifier,
    ) { animatedState ->
        when (animatedState) {
            ArticlesStateUM.Content -> {
                Content(state = state, background = background)
            }
            ArticlesStateUM.Loading -> {
                NewsDetailsPlaceholder(background = background)
            }
            is ArticlesStateUM.LoadingError -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background),
                    contentAlignment = Alignment.Center,
                ) {
                    UnableToLoadData(
                        onRetryClick = animatedState.onRetryClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 35.dp, horizontal = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Content(state: NewsDetailsUM, background: Color) {
    val isRedesignEnabled = LocalRedesignEnabled.current
    val pagerState = rememberPagerState(
        initialPage = state.selectedArticleIndex,
        pageCount = { state.articles.size },
    )

    if (state.articles.isNotEmpty()) {
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    state.onArticleIndexChanged(page)
                }
        }
    }

    LaunchedEffect(state.selectedArticleIndex) {
        if (pagerState.currentPage != state.selectedArticleIndex) {
            pagerState.scrollToPage(state.selectedArticleIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .conditionalCompose(
                condition = isRedesignEnabled,
                modifier = {
                    hazeSourceTangem(zIndex = 1f)
                },
            )
            .background(background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.articles.isNotEmpty()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val article = state.articles[page]
                    ArticleDetail(
                        article = article,
                        modifier = Modifier.fillMaxSize(),
                        onLikeClick = { state.onLikeClick(article.id) },
                        relatedTokensUM = state.relatedTokensUM,
                    )
                }
                if (state.articles.size > 1) {
                    if (isRedesignEnabled) {
                        TangemPagerIndicator(
                            pagerState = pagerState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars),
                            colors = TangemPagerIndicatorColors.copy(
                                overlay = TangemTheme.colors2.tabs.backgroundSecondary.copy(alpha = .1f),
                            ),
                        )
                    } else {
                        PagerIndicator(
                            pagerState = pagerState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .windowInsetsPadding(WindowInsets.navigationBars),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNewsDetailsContent() {
    TangemThemePreview {
        val background = TangemTheme.colors.background.tertiary
        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(background) },
        ) {
            NewsDetailsContent(
                state = NewsDetailsUM(
                    articlesStateUM = ArticlesStateUM.Content,
                    articles = MockArticlesFactory.createMockArticles(),
                    selectedArticleIndex = 0,
                    onShareClick = {},
                    onLikeClick = {},
                    onBackClick = {},
                    onArticleIndexChanged = {},
                ),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNewsDetailsContentV2() {
    TangemThemePreviewRedesign {
        val background = TangemTheme.colors.background.tertiary
        CompositionLocalProvider(
            LocalMainBottomSheetColor provides remember { mutableStateOf(background) },
        ) {
            NewsDetailsContent(
                state = NewsDetailsUM(
                    articlesStateUM = ArticlesStateUM.Content,
                    articles = MockArticlesFactory.createMockArticles(),
                    selectedArticleIndex = 0,
                    onShareClick = {},
                    onLikeClick = {},
                    onBackClick = {},
                    onArticleIndexChanged = {},
                ),
            )
        }
    }
}