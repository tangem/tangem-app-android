package com.tangem.features.news.details.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.news.ArticleHeader
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.appbar.TangemTopAppBar
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.pager.PagerIndicator
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.news.details.impl.MockArticlesFactory

// TODO [REDACTED_TASK_KEY] make internal
@Composable
fun NewsDetailsContent(state: NewsDetailsUM, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(
        initialPage = state.selectedArticleIndex,
        pageCount = { state.articles.size },
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.secondary)
            .systemBarsPadding(),
    ) {
        Column {
            TangemTopAppBar(
                title = null,
                startButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_back_24,
                    onClicked = onBackClick,
                ),
                endButton = TopAppBarButtonUM.Icon(
                    iconRes = R.drawable.ic_share_24,
                    onClicked = state.onShareClick,
                ),
            )
            Box(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (state.articles.isNotEmpty()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                    ) { page ->
                        ArticleDetail(
                            article = state.articles[page],
                            modifier = Modifier.fillMaxSize(),
                            onLikeClick = state.onLikeClick,
                        )
                    }

                    if (state.articles.size > 1) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                        ) {
                            PagerIndicator(
                                pagerState = pagerState,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(bottom = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun ArticleDetail(article: ArticleUM, modifier: Modifier = Modifier, onLikeClick: () -> Unit) {
    val density = LocalDensity.current
    val pagerHeight = 48.dp
    val contentPadding = 56.dp
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        contentPadding = PaddingValues(
            bottom = contentPadding + pagerHeight + WindowInsets.navigationBars.getBottom(density).dp,
        ),
    ) {
        item {
            ArticleHeader(
                title = article.title,
                createdAt = article.createdAt,
                score = article.score,
                tags = article.tags,
                modifier = Modifier.padding(top = 16.dp),
            )

            if (article.shortContent.isNotEmpty()) {
                QuickRecap(
                    content = article.shortContent,
                    modifier = Modifier.padding(top = 32.dp),
                )
            }

            Text(
                text = article.content,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

            SecondaryButtonIconStart(
                iconResId = R.drawable.ic_heart_20,
                text = "Like", // TODO [REDACTED_TASK_KEY] export to strings
                size = TangemButtonSize.RoundedAction,
                onClick = onLikeClick,
            )

            // TODO [REDACTED_TASK_KEY] add related tokens block

            if (article.sources.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Row {
                    Text(
                        text = "Sources", // TODO [REDACTED_TASK_KEY] export to strings
                        style = TangemTheme.typography.h3,
                        color = TangemTheme.colors.text.primary1,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${article.sources.size}",
                        style = TangemTheme.typography.h3,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
            }
        }

        if (article.sources.isNotEmpty()) {
            item {
                val sourcesPagerState = rememberPagerState(
                    pageCount = { article.sources.size },
                )
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalPager(
                    state = sourcesPagerState,
                    modifier = Modifier
                        .fillMaxWidth(),
                    pageSpacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                ) { page ->
                    SourceItem(
                        source = article.sources[page],
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun QuickRecap(content: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.height(IntrinsicSize.Min),
    ) {
        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = 2.dp,
            color = TangemTheme.colors.stroke.primary,
        )
        Column(
            modifier = Modifier
                .padding(start = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_quick_recap_16),
                    contentDescription = null,
                    tint = TangemTheme.colors.icon.accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Quick recap", // TODO [REDACTED_TASK_KEY] export to strings
                    style = TangemTheme.typography.subtitle2,
                    color = TangemTheme.colors.text.accent,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
            )
        }
    }
}

@Composable
private fun SourceItem(source: SourceUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = TangemTheme.colors.background.primary,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_explore_16),
                contentDescription = null,
                tint = TangemTheme.colors.icon.informative,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = source.sourceName,
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        if (source.title.isNotEmpty()) {
            Text(
                text = source.title,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Text(
            text = source.publishedAt,
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewNewsDetailsContent() {
    TangemThemePreview {
        NewsDetailsContent(
            state = NewsDetailsUM(
                articles = MockArticlesFactory.createMockArticles(),
                selectedArticleIndex = 0,
                onShareClick = { },
                onLikeClick = { },
            ),
            onBackClick = { },
        )
    }
}