package com.tangem.features.feed.ui.feed.components.articles

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_NO
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.LocalIsInDarkTheme
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

@Composable
internal fun ArticleCardV2(
    articleConfigUM: ArticleConfigUM,
    onArticleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (articleConfigUM.isTrending) {
        TrendingArticle(
            modifier = modifier,
            articleConfigUM = articleConfigUM,
            onArticleClick = onArticleClick,
        )
    } else {
        DefaultArticle(
            modifier = modifier,
            articleConfigUM = articleConfigUM,
            onArticleClick = onArticleClick,
        )
    }
}

@Composable
private fun TrendingArticle(
    articleConfigUM: ArticleConfigUM,
    onArticleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TrendingArticleBackground(
        modifier = modifier,
        onClick = onArticleClick,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            DayAndRatingInfo(rating = stringReference("${articleConfigUM.score}"))

            SpacerH(8.dp)

            Text(
                text = articleConfigUM.title,
                color = if (articleConfigUM.isViewed) {
                    TangemTheme.colors2.text.neutral.tertiary
                } else {
                    TangemTheme.colors2.text.neutral.primary
                },
                style = TangemTheme.typography2.headingSemibold20,
                textAlign = TextAlign.Start,
            )

            SpacerH(18.dp)

            Text(
                text = articleConfigUM.createdAt.resolveReference(),
                style = TangemTheme.typography2.captionSemibold12,
                color = TangemTheme.colors2.text.neutral.secondary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            SpacerH(18.dp)

            Tags(tags = articleConfigUM.tags.toImmutableList())
        }
    }
}

@Composable
internal fun ShowMoreArticlesCardV2(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .background(color = TangemTheme.colors2.surface.level3)
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.primary,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(vertical = 41.dp, horizontal = 16.dp),
    ) {
        Image(
            modifier = Modifier.size(40.dp),
            imageVector = ImageVector.vectorResource(R.drawable.ic_show_more_news_48),
            contentDescription = stringResourceSafe(R.string.common_show_more),
        )

        SpacerH(10.dp)

        Text(
            text = stringResourceSafe(R.string.news_all_news),
            style = TangemTheme.typography2.bodyRegular16,
            color = TangemTheme.colors2.text.neutral.primary,
        )

        SpacerH(4.dp)

        Text(
            text = stringResourceSafe(R.string.news_stay_in_the_loop),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.secondary,
        )
    }
}

@Composable
private fun DefaultArticle(
    articleConfigUM: ArticleConfigUM,
    onArticleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color = TangemTheme.colors2.surface.level3)
            .clickable(onClick = onArticleClick)
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.primary,
                shape = RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RatingInfo(
                rating = stringReference("${articleConfigUM.score}"),
                isTrending = false,
            )
        }

        SpacerH(8.dp)

        Text(
            modifier = Modifier.weight(1f),
            text = articleConfigUM.title,
            color = if (articleConfigUM.isViewed) {
                TangemTheme.colors2.text.neutral.tertiary
            } else {
                TangemTheme.colors2.text.neutral.primary
            },
            style = TangemTheme.typography2.bodyRegular16,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        SpacerH(8.dp)

        Text(
            text = articleConfigUM.createdAt.resolveReference(),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.secondary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )

        SpacerH(8.dp)

        Tags(tags = articleConfigUM.tags.toImmutableList())
    }
}

@Suppress("MagicNumber")
@Composable
private fun TrendingArticleBackground(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDarkTheme = LocalIsInDarkTheme.current

    val bgColor = remember(isDarkTheme) {
        if (isDarkTheme) {
            Color(TRENDING_NIGHT_BG)
        } else {
            Color(TRENDING_LIGHT_BG)
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                drawRect(bgColor)

                val w = size.width
                val h = size.height
                val radiusScale = (w + h) / 2f

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C16F1).copy(alpha = .8f),
                            Color.Transparent,
                        ),
                        center = Offset(w / 2f, 2.4f * h),
                        radius = radiusScale * 1.57f,
                    ),
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3360FF).copy(alpha = .7f),
                            Color.Transparent,
                        ),
                        center = Offset(w / 2f, 2.95f * h),
                        radius = radiusScale * 1.9f,
                    ),
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF9408).copy(alpha = .45f),
                            Color.Transparent,
                        ),
                        center = Offset(-0.38f * w, 2.1f * h),
                        radius = radiusScale * 1.41f,
                    ),
                )

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFC2424).copy(alpha = .5f),
                            Color.Transparent,
                        ),
                        center = Offset(1.188f * w, 2.37f * h),
                        radius = radiusScale * 1.41f,
                    ),
                )
            }
            .border(width = 1.dp, color = bgColor.copy(.1f))
            .clickable(onClick = onClick),
        content = content,
    )
}

@Composable
private fun DayAndRatingInfo(rating: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RatingInfo(rating = rating, isTrending = true)

        SpacerW(8.dp)

        Text(
            text = stringResourceSafe(R.string.feed_trending_now),
            style = TangemTheme.typography2.captionSemibold12,
            color = TangemTheme.colors2.text.neutral.primary,
        )
    }
}

@Composable
private fun RatingInfo(rating: TextReference, isTrending: Boolean) {
    Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_wrapped_circle_star_16),
        tint = if (isTrending) {
            TangemTheme.colors2.fill.status.attention
        } else {
            TangemTheme.colors2.markers.iconGray
        },
        contentDescription = null,
    )

    SpacerW(2.dp)

    Text(
        text = rating.resolveReference(),
        color = if (isTrending) {
            TangemTheme.colors2.text.status.attention
        } else {
            TangemTheme.colors2.text.neutral.secondary
        },
        style = TangemTheme.typography2.captionSemibold12,
    )
}

private const val TRENDING_NIGHT_BG = 0xFF1F1F1F
private const val TRENDING_LIGHT_BG = 0xFFFFFFFF

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TagsPreview() {
    TangemThemePreviewRedesign {
        Tags(
            tags = persistentListOf(
                LabelUM(TextReference.Str("Hype")),
                LabelUM(TextReference.Str("BTC")),
                LabelUM(TextReference.Str("Supply")),
                LabelUM(TextReference.Str("Demand")),
                LabelUM(TextReference.Str("Best rate")),
                LabelUM(TextReference.Str("Breaking news")),
            ),
        )
    }
}

@Preview(widthDp = 360, uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 360, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun ArticleCardsPreview() {
    val tags = listOf(
        LabelUM(TextReference.Str("Hype")),
        LabelUM(TextReference.Str("BTC")),
        LabelUM(TextReference.Str("Supply")),
        LabelUM(TextReference.Str("Demand")),
        LabelUM(TextReference.Str("Breaking news")),
    ).toImmutableSet()

    val config = ArticleConfigUM(
        id = 1,
        title = "Bitcoin ETFs log 4th straight day of inflows (+\$550M)",
        score = 9.5f,
        createdAt = TextReference.Str("1h ago"),
        isTrending = true,
        tags = tags,
        isViewed = false,
    )

    TangemThemePreviewRedesign {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ArticleCardV2(
                articleConfigUM = config,
                onArticleClick = {},
            )

            SpacerH(20.dp)

            ArticleCardV2(
                articleConfigUM = config.copy(isViewed = true),
                onArticleClick = {},
            )

            SpacerH(20.dp)

            ArticleCardV2(
                articleConfigUM = config.copy(isTrending = false),
                onArticleClick = {},
            )

            SpacerH(20.dp)

            ArticleCardV2(
                articleConfigUM = config.copy(isTrending = false, isViewed = true),
                onArticleClick = {},
            )

            SpacerH(20.dp)

            ShowMoreArticlesCardV2(onClick = {})
        }
    }
}