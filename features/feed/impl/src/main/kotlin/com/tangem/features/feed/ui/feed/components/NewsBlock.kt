package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.tangem.features.feed.ui.feed.components.articles.ArticleCard
import com.tangem.features.feed.ui.feed.components.articles.ArticleConfigUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.state.*

internal const val FOURTH_ITEM_INDEX = 3
private const val GRADIENT_START = 0f
private const val GRADIENT_END = 0.5f
private const val LINEAR_GRADIENT_FIRST_PART_V2 = 0xFFA3A0FF
private const val LINEAR_GRADIENT_SECOND_PART_V2 = 0xFFF79DFF

private const val LINEAR_GRADIENT_FIRST_PART_V1 = 0xFF635EEC
private const val LINEAR_GRADIENT_SECOND_PART_V1 = 0xFFE05AED

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
    val isRedesignEnabled = LocalRedesignEnabled.current
    val gradientStart = remember(isRedesignEnabled) {
        if (isRedesignEnabled) {
            Color(LINEAR_GRADIENT_FIRST_PART_V2)
        } else {
            Color(LINEAR_GRADIENT_FIRST_PART_V1)
        }
    }

    val gradientEnd = remember(isRedesignEnabled) {
        if (isRedesignEnabled) {
            Color(LINEAR_GRADIENT_SECOND_PART_V2)
        } else {
            Color(LINEAR_GRADIENT_SECOND_PART_V1)
        }
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
                                        GRADIENT_START to gradientStart,
                                        GRADIENT_END to gradientEnd,
                                    ),
                                ),
                            ) {
                                append(stringResourceSafe(R.string.feed_tangem_ai))
                            }
                        },
                        style = TangemTheme.typography.subtitle1,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            },
            onSeeAllClick = { feedListCallbacks.onOpenAllNews(false) },
            isLoading = news.newsUMState == NewsUMState.LOADING,
            shouldShowSeeAll = news.newsUMState == NewsUMState.CONTENT,
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

        NewsSlider(
            NewsSliderConfig(
                callbacks = NewsSliderCallbacks(
                    onOpenAllNews = { feedListCallbacks.onOpenAllNews(true) },
                    onSliderScroll = feedListCallbacks.onSliderScroll,
                    onSliderEndReached = feedListCallbacks.onSliderEndReached,
                    onArticleClick = feedListCallbacks.onArticleClick,
                ),
                content = news.content,
                shouldShowSeeAllNewsItem = true,
            ),
        )

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
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                }
            },
            onSeeAllClick = {},
            shouldShowSeeAll = false,
            isLoading = false,
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