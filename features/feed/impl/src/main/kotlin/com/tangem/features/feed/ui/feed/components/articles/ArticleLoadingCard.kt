package com.tangem.features.feed.ui.feed.components.articles

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun TrendingLoadingArticle(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors3.bg.secondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ArticleShimmerLine(
                style = TangemTheme.typography3.caption.medium,
                width = 46.dp,
            )
            SpacerH(8.dp)
            ArticleShimmerLine(
                style = TangemTheme.typography3.heading.small,
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(48.dp)
            ArticleShimmerLine(
                style = TangemTheme.typography3.caption.medium,
                width = 46.dp,
            )
            SpacerH(12.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ArticleTagShimmer(width = 58.dp)
                ArticleTagShimmer(width = 58.dp)
                ArticleTagShimmer(width = 58.dp)
            }
        }
    }
}

@Composable
fun DefaultLoadingArticle(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors3.bg.secondary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            ArticleShimmerLine(
                style = TangemTheme.typography3.caption.medium,
                width = 46.dp,
            )
            SpacerH(8.dp)
            ArticleShimmerLine(
                style = TangemTheme.typography3.body.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            SpacerH(44.dp)
            ArticleShimmerLine(
                style = TangemTheme.typography3.caption.medium,
                width = 46.dp,
            )
            SpacerH(12.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ArticleTagShimmer(width = 72.dp)
                ArticleTagShimmer(width = 62.dp)
                ArticleTagShimmer(width = 32.dp)
            }
        }
    }
}

@Composable
private fun ArticleShimmerLine(style: TextStyle, modifier: Modifier = Modifier, width: Dp? = null) {
    val lineHeight = with(LocalDensity.current) { style.lineHeight.toDp() }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun ArticleTagShimmer(width: Dp, modifier: Modifier = Modifier) {
    TangemShimmer(
        radius = 999.dp,
        modifier = modifier.size(width = width, height = 24.dp),
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TrendingLoadingArticlePreview() {
    TangemThemePreviewRedesign {
        TrendingLoadingArticle()
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DefaultLoadingArticlePreview() {
    TangemThemePreviewRedesign {
        DefaultLoadingArticle()
    }
}