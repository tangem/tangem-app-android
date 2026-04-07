package com.tangem.features.feed.ui.news.details.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.BottomFade
import com.tangem.core.ui.components.BottomFadeWithBlur
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.components.articles.ArticleHeader
import com.tangem.features.feed.ui.news.details.state.ArticleUM
import com.tangem.features.feed.ui.news.details.state.RelatedArticleUM
import com.tangem.features.feed.ui.news.details.state.RelatedTokensUM
import dev.chrisbanes.haze.rememberHazeState

@Composable
internal fun ArticleDetail(
    contentPadding: PaddingValues,
    article: ArticleUM,
    onLikeClick: () -> Unit,
    relatedTokensUM: RelatedTokensUM,
    modifier: Modifier = Modifier,
) {
    if (LocalRedesignEnabled.current) {
        ArticleDetailV2(
            contentPadding = contentPadding,
            article = article,
            onLikeClick = onLikeClick,
            relatedTokensUM = relatedTokensUM,
            modifier = modifier,
        )
    } else {
        ArticleDetailV1(
            article = article,
            onLikeClick = onLikeClick,
            relatedTokensUM = relatedTokensUM,
            modifier = modifier,
        )
    }
}

@Suppress("LongMethod")
@Composable
private fun ArticleDetailV1(
    article: ArticleUM,
    onLikeClick: () -> Unit,
    relatedTokensUM: RelatedTokensUM,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
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
                    isTrending = article.isTrending,
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
                    iconResId = if (article.isLiked) {
                        R.drawable.ic_heart_filled_20
                    } else {
                        R.drawable.ic_heart_20
                    },
                    iconTint = Color.Unspecified,
                    text = stringResourceSafe(R.string.news_like),
                    size = TangemButtonSize.RoundedAction,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLikeClick()
                    },
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

@Suppress("LongMethod")
@Composable
internal fun ArticleDetailV2(
    contentPadding: PaddingValues,
    article: ArticleUM,
    onLikeClick: () -> Unit,
    relatedTokensUM: RelatedTokensUM,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val background = LocalMainBottomSheetColor.current.value
    val pagerHeight = 32.dp
    val bottomPadding = pagerHeight + 56.dp + with(density) {
        WindowInsets.navigationBars.getBottom(this).div(this.density)
    }.dp

    CompositionLocalProvider(LocalHazeState provides rememberHazeState()) {
        Box(modifier = modifier) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSourceTangem(zIndex = -1f)
                    .background(background),
                contentPadding = PaddingValues(bottom = bottomPadding, top = contentPadding.calculateTopPadding()),
            ) {
                item("content") {
                    ArticleHeader(
                        title = article.title,
                        createdAt = article.createdAt.resolveReference(),
                        score = article.score,
                        tags = article.tags,
                        isTrending = article.isTrending,
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
                        style = TangemTheme.typography2.bodyRegular16,
                        color = TangemTheme.colors2.text.neutral.primary,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .padding(horizontal = 16.dp),
                    )

                    SpacerH(24.dp)

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = TangemTheme.colors2.border.neutral.primary,
                    )

                    SpacerH(20.dp)

                    SecondaryTangemButton(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        text = resourceReference(R.string.news_like),
                        size = com.tangem.core.ui.ds.button.TangemButtonSize.X9,
                        tangemIconUM = if (article.isLiked) {
                            TangemIconUM.Icon(
                                iconRes = R.drawable.ic_heart_filled_20,
                                tintReference = { TangemTheme.colors2.markers.iconRed },
                            )
                        } else {
                            TangemIconUM.Icon(
                                iconRes = R.drawable.ic_heart_20,
                                tintReference = { TangemTheme.colors2.button.iconPrimary },
                            )
                        },
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLikeClick()
                        },
                        shape = TangemButtonShape.Rounded,
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
                        SpacerH(24.dp)
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResourceSafe(R.string.news_sources),
                                style = TangemTheme.typography2.headingSemibold20,
                                color = TangemTheme.colors2.text.neutral.primary,
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

            BottomFadeWithBlur(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(80.dp)
                    .fillMaxWidth(),
                backgroundColor = background,
            )
        }
    }
}