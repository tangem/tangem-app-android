package com.tangem.common.ui.news

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

@Composable
fun ArticleCard(
    articleConfigUM: ArticleConfigUM,
    onArticleClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = TangemBlockCardColors,
) {
    if (articleConfigUM.isTrending) {
        TrendingArticle(
            articleConfigUM = articleConfigUM,
            modifier = modifier,
            onArticleClick = onArticleClick,
            colors = colors,
        )
    } else {
        DefaultArticle(
            articleConfigUM = articleConfigUM,
            modifier = modifier,
            onArticleClick = onArticleClick,
            colors = colors,
        )
    }
}

@Composable
private fun TrendingArticle(
    articleConfigUM: ArticleConfigUM,
    onArticleClick: () -> Unit,
    colors: CardColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(colors.containerColor)
            .clickable { onArticleClick() }
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier
                .background(
                    color = TangemTheme.colors.icon.accent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            text = stringResourceSafe(R.string.feed_trending_now),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.icon.accent,
        )

        SpacerH(12.dp)

        Text(
            text = articleConfigUM.title,
            color = if (articleConfigUM.isViewed) {
                TangemTheme.colors.text.tertiary
            } else {
                TangemTheme.colors.text.primary1
            },
            style = TangemTheme.typography.h3,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        SpacerH(8.dp)

        ArticleInfo(
            score = articleConfigUM.score,
            createdAt = articleConfigUM.createdAt.resolveReference(),
        )

        SpacerH(32.dp)

        Tags(
            modifier = Modifier.padding(horizontal = 46.dp),
            tags = articleConfigUM.tags.toImmutableList(),
        )
    }
}

@Composable
fun ShowMoreArticlesCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    BlockCard(
        modifier = modifier,
        onClick = onClick,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 31.dp, horizontal = 12.dp),
        ) {
            Image(
                imageVector = ImageVector.vectorResource(R.drawable.ic_show_more_news_48),
                contentDescription = stringResourceSafe(R.string.common_show_more),
            )

            SpacerH(16.dp)

            Text(
                text = stringResourceSafe(R.string.news_all_news),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )

            Text(
                text = stringResourceSafe(R.string.news_stay_in_the_loop),
                style = TangemTheme.typography.caption2,
                color = TangemTheme.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun DefaultArticle(
    articleConfigUM: ArticleConfigUM,
    onArticleClick: () -> Unit,
    colors: CardColors,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(colors.containerColor)
            .clickable { onArticleClick() }
            .padding(12.dp),
    ) {
        ArticleInfo(
            score = articleConfigUM.score,
            createdAt = articleConfigUM.createdAt.resolveReference(),
        )

        SpacerH(8.dp)

        Text(
            text = articleConfigUM.title,
            color = if (articleConfigUM.isViewed) {
                TangemTheme.colors.text.tertiary
            } else {
                TangemTheme.colors.text.primary1
            },
            style = TangemTheme.typography.subtitle1,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        SpacerHMax(modifier = Modifier.padding(bottom = 16.dp))

        Tags(tags = articleConfigUM.tags.toImmutableList())
    }
}

@Composable
private fun Tags(tags: ImmutableList<LabelUM>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = MAX_TAGS_COUNT_TO_SHOW + 1,
        maxLines = 1,
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        val displayedTags = if (tags.size > MAX_TAGS_COUNT_TO_SHOW) {
            tags.take(MAX_TAGS_COUNT_TO_SHOW)
        } else {
            tags
        }

        displayedTags.forEach { tag ->
            Label(state = tag)
        }

        if (tags.size > MAX_TAGS_COUNT_TO_SHOW) {
            val remainingItems = tags.size - MAX_TAGS_COUNT_TO_SHOW
            Label(
                state = LabelUM(
                    text = TextReference.Str("${StringsSigns.PLUS}$remainingItems"),
                    maxLines = 1,
                ),
            )
        }
    }
}

private const val MAX_TAGS_COUNT_TO_SHOW = 3

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TagsPreview() {
    TangemThemePreview {
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

@Preview(showBackground = true, widthDp = 360, backgroundColor = 0xFF000000)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
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

    TangemThemePreview {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ArticleCard(
                articleConfigUM = config,
                onArticleClick = {},
            )

            SpacerH(20.dp)

            ArticleCard(
                articleConfigUM = config.copy(isViewed = true),
                onArticleClick = {},
            )

            SpacerH(20.dp)

            ArticleCard(
                articleConfigUM = config.copy(isTrending = false),
                onArticleClick = {},
            )

            SpacerH(20.dp)

            ArticleCard(
                articleConfigUM = config.copy(isTrending = false, isViewed = true),
                onArticleClick = {},
            )
        }
    }
}