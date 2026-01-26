package com.tangem.features.feed.ui.news.details

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.tangem.common.ui.news.ArticleHeader
import com.tangem.core.ui.R
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.pager.PagerIndicator
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.ui.news.details.components.NewsDetailsPlaceholder
import com.tangem.features.feed.ui.news.details.components.RelatedTokensBlock
import com.tangem.features.feed.ui.news.details.state.*

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
private fun ArticleDetail(
    article: ArticleUM,
    onLikeClick: () -> Unit,
    relatedTokensUM: RelatedTokensUM,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value
    val pagerHeight = 32.dp
    val contentPadding = pagerHeight + 56.dp + with(density) {
        WindowInsets.navigationBars.getBottom(this).div(this.density)
    }.dp

    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding),
        ) {
            item("content") {
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

                if (article.isLiked) {
                    PrimaryButtonIconStart(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        iconResId = R.drawable.ic_heart_20,
                        text = stringResourceSafe(R.string.news_like),
                        size = TangemButtonSize.RoundedAction,
                        onClick = { onLikeClick() },
                    )
                } else {
                    SecondaryButtonIconStart(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        iconResId = R.drawable.ic_heart_20,
                        text = stringResourceSafe(R.string.news_like),
                        size = TangemButtonSize.RoundedAction,
                        onClick = { onLikeClick() },
                    )
                }

                RelatedTokensBlock(
                    relatedTokensUM = relatedTokensUM,
                    onItemClick = when (relatedTokensUM) {
                        is RelatedTokensUM.Content -> relatedTokensUM.onTokenClick
                        else -> null
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                if (article.relatedArticles.isNotEmpty()) {
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
                            text = "${article.relatedArticles.size}",
                            style = TangemTheme.typography.h3,
                            color = TangemTheme.colors.text.tertiary,
                        )
                    }
                }
            }

            if (article.relatedArticles.isNotEmpty()) {
                item("relatedArticles") {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 12.dp),
                        state = rememberLazyListState(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = article.relatedArticles,
                            key = RelatedArticleUM::id,
                        ) { article ->
                            RelatedNewsItem(
                                relatedArticle = article,
                                modifier = Modifier.fillParentMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
        BottomFade(
            modifier = Modifier
                .align(Alignment.BottomCenter),
            backgroundColor = background,
        )
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
private fun RelatedNewsItem(relatedArticle: RelatedArticleUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .sizeIn(maxWidth = 256.dp, minHeight = 132.dp)
            .background(color = TangemTheme.colors.background.action, shape = RoundedCornerShape(12.dp))
            .clickable(onClick = relatedArticle.onClick)
            .padding(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_explore_16),
                        contentDescription = null,
                        tint = TangemTheme.colors.icon.informative,
                        modifier = Modifier.size(16.dp),
                    )
                    SpacerW(4.dp)
                    Text(
                        text = relatedArticle.media.name,
                        style = TangemTheme.typography.caption1,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TangemTheme.colors.text.tertiary,
                    )
                }
                if (relatedArticle.title.isNotEmpty()) {
                    SpacerH(4.dp)
                    Text(
                        text = relatedArticle.title,
                        style = TangemTheme.typography.subtitle2,
                        color = TangemTheme.colors.text.primary1,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (relatedArticle.imageUrl != null) {
                SubcomposeAsyncImage(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop,
                    model = ImageRequest.Builder(context = LocalContext.current)
                        .data(relatedArticle.imageUrl)
                        .crossfade(enable = false)
                        .allowHardware(true)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    loading = {
                        RectangleShimmer(
                            modifier = Modifier.size(40.dp),
                            radius = 4.dp,
                        )
                    },
                    error = {},
                    contentDescription = relatedArticle.media.name,
                )
            }
        }
        SpacerHMax()
        Text(
            text = relatedArticle.publishedAt.resolveReference(),
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