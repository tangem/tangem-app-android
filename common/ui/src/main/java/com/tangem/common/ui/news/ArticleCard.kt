package com.tangem.common.ui.news

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.utils.StringsSigns
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableSet

@Composable
fun ArticleCard(articleConfigUM: ArticleConfigUM, onArticleClick: () -> Unit, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        onClick = onArticleClick,
    ) {
        if (articleConfigUM.isTrending) {
            TrendingArticle(articleConfigUM = articleConfigUM)
        } else {
            DefaultArticle(articleConfigUM = articleConfigUM)
        }
    }
}

@Composable
private fun TrendingArticle(articleConfigUM: ArticleConfigUM) {
    Column(
        modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
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
        )

        SpacerH(8.dp)

        ArticleInfo(
            score = articleConfigUM.score,
            createdAt = articleConfigUM.createdAt,
        )

        SpacerH(32.dp)

        Tags(
            modifier = Modifier.padding(horizontal = 46.dp),
            tags = articleConfigUM.tags.toImmutableList(),
        )
    }
}

@Composable
private fun DefaultArticle(articleConfigUM: ArticleConfigUM) {
    Column(modifier = Modifier.padding(12.dp)) {
        ArticleInfo(
            score = articleConfigUM.score,
            createdAt = articleConfigUM.createdAt,
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

        SpacerH(16.dp)

        Tags(tags = articleConfigUM.tags.toImmutableList())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Tags(tags: ImmutableList<LabelUM>, modifier: Modifier = Modifier) {
    val expandIndicator = remember {
        ContextualFlowRowOverflow.expandIndicator {
            val remainingItems = tags.size - shownItemCount
            Label(state = LabelUM(TextReference.Str("${StringsSigns.PLUS}$remainingItems")))
        }
    }
    ContextualFlowRow(
        itemCount = tags.size,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxLines = 1,
        overflow = expandIndicator,
    ) { index ->
        Label(state = tags[index])
    }
}

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
        LabelUM(TextReference.Str("Best rate")),
        LabelUM(TextReference.Str("Breaking news")),
    ).toImmutableSet()

    val config = ArticleConfigUM(
        id = 1,
        title = "Bitcoin ETFs log 4th straight day of inflows (+\$550M)",
        score = 9.5f,
        createdAt = "1h ago",
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