package com.tangem.features.feed.ui.feed.components.articles

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign

@Composable
fun TrendingLoadingArticle(modifier: Modifier = Modifier) {
    TrendingLoadingArticleV2(modifier)
}

@Composable
private fun TrendingLoadingArticleV2(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors2.surface.level3),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            RectangleShimmer(modifier = Modifier.size(width = 46.dp, height = 16.dp), radius = TangemTheme.dimens2.x25)
            SpacerH(8.dp)
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp),
                radius = TangemTheme.dimens2.x25,
            )
            SpacerH(48.dp)
            RectangleShimmer(modifier = Modifier.size(width = 46.dp, height = 16.dp), radius = TangemTheme.dimens2.x25)
            SpacerH(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RectangleShimmer(
                    modifier = Modifier.size(width = 58.dp, height = 24.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                RectangleShimmer(
                    modifier = Modifier.size(width = 58.dp, height = 24.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                RectangleShimmer(
                    modifier = Modifier.size(width = 58.dp, height = 24.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }
        }
    }
}

@Composable
fun DefaultLoadingArticle(modifier: Modifier = Modifier) {
    DefaultLoadingArticleV2(modifier)
}

@Composable
private fun DefaultLoadingArticleV2(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors2.surface.level3),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            RectangleShimmer(modifier = Modifier.size(width = 46.dp, height = 16.dp), radius = TangemTheme.dimens2.x25)
            SpacerH(8.dp)
            RectangleShimmer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                radius = TangemTheme.dimens2.x25,
            )
            SpacerH(44.dp)
            RectangleShimmer(modifier = Modifier.size(width = 46.dp, height = 16.dp), radius = TangemTheme.dimens2.x25)
            SpacerH(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RectangleShimmer(
                    modifier = Modifier.size(width = 72.dp, height = 24.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                RectangleShimmer(
                    modifier = Modifier.size(width = 62.dp, height = 24.dp),
                    radius = TangemTheme.dimens2.x25,
                )
                RectangleShimmer(
                    modifier = Modifier.size(width = 32.dp, height = 24.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }
        }
    }
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