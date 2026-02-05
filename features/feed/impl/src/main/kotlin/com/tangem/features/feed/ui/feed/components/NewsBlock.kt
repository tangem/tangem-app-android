package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.news.ArticleCard
import com.tangem.common.ui.news.ArticleConfigUM
import com.tangem.common.ui.news.ShowMoreArticlesCard
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.state.FeedListCallbacks
import com.tangem.features.feed.ui.feed.state.NewsUM
import com.tangem.features.feed.ui.feed.state.NewsUMState

private const val FOURTH_ITEM_INDEX = 3
private const val GRADIENT_START = 0f
private const val GRADIENT_END = 0.5f
private val LinearGradientFirstPart = Color(0xFF635EEC)
private val LinearGradientSecondPart = Color(0xFFE05AED)

@Composable
internal fun NewsBlock(feedListCallbacks: FeedListCallbacks, news: NewsUM, trendingArticle: ArticleConfigUM?) {
    AnimatedContent(news.newsUMState) { newsUMState ->
        when (newsUMState) {
            NewsUMState.LOADING -> NewsLoadingBlock()
            NewsUMState.CONTENT -> {
                if (news.content.isNotEmpty()) {
                    NewsContentBlock(
                        feedListCallbacks = feedListCallbacks,
                        news = news,
                        trendingArticle = trendingArticle,
                    )
                }
            }
            NewsUMState.ERROR -> NewsErrorBlock(onRetryClick = news.onRetryClicked)
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun NewsContentBlock(feedListCallbacks: FeedListCallbacks, news: NewsUM, trendingArticle: ArticleConfigUM?) {
    val listState = rememberLazyListState()
    val articlesReadStatus = remember(news.content) {
        news.content.map { it.isViewed }
    }
    LaunchedEffect(articlesReadStatus) {
        listState.requestScrollToItem(0)
    }
    Column {
        Header(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResourceSafe(R.string.common_news),
                        style = TangemTheme.typography.h3,
                        color = TangemTheme.colors.text.primary1,
                    )

                    SpacerW(4.dp)

                    Image(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_stars_20),
                        contentDescription = null,
                    )

                    SpacerW(2.dp)

                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle().copy(
                                    brush = Brush.linearGradient(
                                        GRADIENT_START to LinearGradientFirstPart,
                                        GRADIENT_END to LinearGradientSecondPart,
                                    ),
                                ),
                            ) {
                                append(stringResourceSafe(R.string.feed_tangem_ai))
                            }
                        },
                        style = TangemTheme.typography.subtitle1,
                    )
                }
            },
            onSeeAllClick = { feedListCallbacks.onOpenAllNews(false) },
        )
        SpacerH(12.dp)

        if (trendingArticle != null) {
            Column {
                ArticleCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    articleConfigUM = trendingArticle,
                    onArticleClick = { feedListCallbacks.onArticleClick(trendingArticle.id) },
                    colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
                )
                SpacerH(12.dp)
            }
        }

        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            state = listState,
        ) {
            itemsIndexed(
                items = news.content,
                key = { _, article -> article.id },
                contentType = { _, _ -> "article" },
            ) { index, article ->
                val articleModifier = if (index == FOURTH_ITEM_INDEX) {
                    Modifier.onFirstVisible(
                        minFractionVisible = 0.5f,
                        callback = feedListCallbacks.onSliderScroll,
                    )
                } else {
                    Modifier
                }
                ArticleCard(
                    articleConfigUM = article,
                    onArticleClick = { feedListCallbacks.onArticleClick(article.id) },
                    modifier = articleModifier
                        .heightIn(min = 164.dp)
                        .width(216.dp),
                    colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
                )
            }

            item(contentType = "show_more") {
                ShowMoreArticlesCard(
                    modifier = Modifier
                        .width(216.dp)
                        .heightIn(min = 164.dp)
                        .onFirstVisible(
                            minFractionVisible = 0.5f,
                            callback = feedListCallbacks.onSliderEndReached,
                        ),
                    onClick = { feedListCallbacks.onOpenAllNews(true) },
                )
            }
        }
        SpacerH(32.dp)
    }
}

@Composable
private fun NewsErrorBlock(onRetryClick: () -> Unit) {
    Column {
        Header(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResourceSafe(R.string.common_news),
                        style = TangemTheme.typography.h3,
                        color = TangemTheme.colors.text.primary1,
                    )
                }
            },
            onSeeAllClick = {},
        )
        SpacerH(12.dp)
        BlockCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
        ) {
            UnableToLoadData(
                onRetryClick = onRetryClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 35.dp, horizontal = 10.dp),
            )
        }
        SpacerH(32.dp)
    }
}