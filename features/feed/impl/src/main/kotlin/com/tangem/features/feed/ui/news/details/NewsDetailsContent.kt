package com.tangem.features.feed.ui.news.details

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.news.ArticleHeader
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.pager.PagerIndicator
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.MockArticlesFactory
import com.tangem.features.feed.ui.news.details.state.NewsDetailsUM
import com.tangem.features.feed.ui.news.details.state.SourceUM

@Composable
internal fun NewsDetailsContent(state: NewsDetailsUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    val pagerState = rememberPagerState(
        initialPage = state.selectedArticleIndex,
        pageCount = { state.articles.size },
    )

    LaunchedEffect(state.selectedArticleIndex) {
        if (pagerState.currentPage != state.selectedArticleIndex) {
            pagerState.scrollToPage(state.selectedArticleIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != state.selectedArticleIndex) {
            state.onArticleIndexChanged(pagerState.currentPage)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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

@Suppress("LongMethod")
@Composable
private fun ArticleDetail(article: ArticleUM, modifier: Modifier = Modifier, onLikeClick: () -> Unit) {
    val density = LocalDensity.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 56.dp + WindowInsets.navigationBars.getBottom(density).dp),
    ) {
        item {
            ArticleHeader(
                title = article.title,
                createdAt = article.createdAt.resolveReference(),
                score = article.score,
                tags = article.tags,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
            )

            if (article.shortContent.isNotEmpty()) {
                QuickRecap(
                    content = article.shortContent,
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .padding(horizontal = 16.dp),
                )
            }

            Text(
                text = article.content,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .padding(horizontal = 16.dp),
            )

            SpacerH(24.dp)

            SecondaryButtonIconStart(
                modifier = Modifier.padding(horizontal = 16.dp),
                iconResId = R.drawable.ic_heart_20,
                text = stringResourceSafe(R.string.news_like),
                size = TangemButtonSize.RoundedAction,
                onClick = onLikeClick,
            )

            // TODO [REDACTED_TASK_KEY] add related tokens block

            if (article.sources.isNotEmpty()) {
                SpacerH(24.dp)
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResourceSafe(R.string.news_sources),
                        style = TangemTheme.typography.h3,
                        color = TangemTheme.colors.text.primary1,
                    )
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
                LazyRow(
                    modifier = Modifier.padding(vertical = 12.dp),
                    state = rememberLazyListState(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(
                        items = article.sources,
                        key = SourceUM::id,
                    ) { source ->
                        SourceItem(source = source)
                    }
                }
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
                    text = stringResourceSafe(R.string.news_quick_recap),
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
            .widthIn(max = 216.dp)
            .heightIn(min = 132.dp)
            .background(
                color = TangemTheme.colors.background.action,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = source.onClick)
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
                modifier = Modifier.size(16.dp),
            )
            SpacerW(4.dp)
            Text(
                text = source.source.name,
                style = TangemTheme.typography.caption1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = TangemTheme.colors.text.tertiary,
            )
        }
        if (source.title.isNotEmpty()) {
            SpacerH(4.dp)
            Text(
                text = source.title,
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary1,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Text(
            text = source.publishedAt.resolveReference(),
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
                onShareClick = {},
                onLikeClick = {},
                onBackClick = {},
            ),
        )
    }
}