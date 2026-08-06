package com.tangem.features.feed.ui.news.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.components.articles.ArticleHeader
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.RelatedArticleUM
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM
import dev.chrisbanes.haze.HazeState
import kotlinx.collections.immutable.ImmutableList

private val ARTICLE_DETAIL_PAGER_HEIGHT = 32.dp

@Composable
internal fun ArticleDetail(
    hazeState: HazeState,
    contentPadding: PaddingValues,
    article: ArticleUM,
    onLikeClick: () -> Unit,
    relatedTokensUM: RelatedTokensUM,
    modifier: Modifier = Modifier,
) {
    val background = LocalMainBottomSheetColor.current.value
    val bottomPadding = articleDetailBottomPadding()

    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(modifier = modifier) {
            ArticleDetailList(
                article = article,
                relatedTokensUM = relatedTokensUM,
                onLikeClick = onLikeClick,
                background = background,
                hazeState = hazeState,
                contentPadding = contentPadding,
                bottomPadding = bottomPadding,
            )
            ArticleDetailBottomFade(background = background)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun BoxScope.ArticleDetailList(
    article: ArticleUM,
    relatedTokensUM: RelatedTokensUM,
    onLikeClick: () -> Unit,
    background: Color,
    hazeState: HazeState,
    contentPadding: PaddingValues,
    bottomPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .hazeSourceTangem(hazeState)
            .background(background),
        contentPadding = PaddingValues(
            bottom = bottomPadding,
            top = contentPadding.calculateTopPadding(),
        ),
    ) {
        item(key = "content") {
            ArticleDetailMainContent(
                article = article,
                relatedTokensUM = relatedTokensUM,
                onLikeClick = onLikeClick,
            )
        }
        articleDetailRelatedArticlesItems(relatedArticles = article.relatedArticles)
    }
}

@Composable
private fun ArticleDetailMainContent(article: ArticleUM, relatedTokensUM: RelatedTokensUM, onLikeClick: () -> Unit) {
    ArticleHeader(
        title = article.title,
        createdAt = article.createdAt.resolveReference(),
        score = article.score,
        tags = article.tags,
        isTrending = article.isTrending,
        modifier = Modifier
            .padding(top = 6.dp, bottom = 24.dp)
            .padding(horizontal = 16.dp),
    )

    if (article.shortContent.isNotEmpty()) {
        QuickRecap(
            content = article.shortContent,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    ArticleDetailBody(content = article.content)
    ArticleDetailLikeButton(
        isLiked = article.isLiked,
        onLikeClick = onLikeClick,
    )
    RelatedTokensBlock(
        relatedTokensUM = relatedTokensUM,
        onItemClick = when (relatedTokensUM) {
            is RelatedTokensUM.Content -> relatedTokensUM.onTokenClick
            else -> null
        },
        modifier = Modifier.padding(horizontal = 16.dp),
    )

    if (article.relatedArticles.isNotEmpty()) {
        ArticleDetailRelatedSourcesHeader()
    }
}

@Composable
private fun ArticleDetailBody(content: String) {
    Text(
        text = content,
        style = TangemTheme.typography3.body.medium,
        color = TangemTheme.colors3.text.primary,
        modifier = Modifier.padding(vertical = 20.dp, horizontal = 24.dp),
    )
}

@Composable
private fun ArticleDetailLikeButton(isLiked: Boolean, onLikeClick: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current

    TangemButton(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = resourceReference(R.string.news_like),
        variant = TangemButton.Variant.Secondary,
        onClick = {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            onLikeClick()
        },
        iconStart = if (isLiked) {
            TangemIconUM.Icon(
                iconRes = R.drawable.ic_heart_filled_20,
                tintReference = { TangemTheme.colors3.icon.accent.red },
            )
        } else {
            TangemIconUM.Icon(iconRes = R.drawable.ic_heart_20)
        },
        size = TangemButton.Size.X9,
    )
}

@Composable
private fun ArticleDetailRelatedSourcesHeader() {
    SpacerH(16.dp)
    Text(
        modifier = Modifier.padding(horizontal = 24.dp),
        text = stringResourceSafe(R.string.news_sources),
        style = TangemTheme.typography3.heading.small,
        color = TangemTheme.colors3.text.primary,
    )
}

private fun LazyListScope.articleDetailRelatedArticlesItems(relatedArticles: ImmutableList<RelatedArticleUM>) {
    if (relatedArticles.isEmpty()) return

    item(key = "relatedArticles") {
        LazyRow(
            modifier = Modifier.padding(vertical = 12.dp),
            state = rememberLazyListState(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(
                items = relatedArticles,
                key = RelatedArticleUM::id,
            ) { relatedArticle ->
                RelatedNewsItem(
                    relatedArticle = relatedArticle,
                    modifier = Modifier.fillParentMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.ArticleDetailBottomFade(background: Color) {
    BottomFade(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth(),
        backgroundColor = background,
        height = 60.dp,
    )
}

@Composable
private fun articleDetailBottomPadding(): Dp {
    val density = LocalDensity.current
    return ARTICLE_DETAIL_PAGER_HEIGHT + 16.dp + with(density) {
        WindowInsets.navigationBars.getBottom(this).div(this.density)
    }.dp
}