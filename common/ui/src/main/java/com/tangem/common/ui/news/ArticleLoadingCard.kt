package com.tangem.common.ui.news

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.res.TangemTheme

@Composable
fun TrendingLoadingArticle(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RectangleShimmer(modifier = Modifier.size(width = 96.dp, height = 24.dp), radius = 8.dp)
            SpacerH(12.dp)
            RectangleShimmer(modifier = Modifier.size(width = 285.dp, height = 18.dp), radius = 4.dp)
            SpacerH(6.dp)
            RectangleShimmer(modifier = Modifier.size(width = 190.dp, height = 18.dp), radius = 4.dp)
            SpacerH(14.dp)
            RectangleShimmer(modifier = Modifier.size(width = 110.dp, height = 18.dp), radius = 4.dp)
            SpacerH(32.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RectangleShimmer(modifier = Modifier.size(width = 64.dp, height = 24.dp), radius = 8.dp)
                RectangleShimmer(modifier = Modifier.size(width = 64.dp, height = 24.dp), radius = 8.dp)
                RectangleShimmer(modifier = Modifier.size(width = 64.dp, height = 24.dp), radius = 8.dp)
            }
        }
    }
}

@Composable
fun DefaultLoadingArticle(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            RectangleShimmer(modifier = Modifier.size(width = 110.dp, height = 16.dp), radius = 4.dp)
            SpacerH(12.dp)
            RectangleShimmer(modifier = Modifier.size(width = 142.dp, height = 18.dp), radius = 4.dp)
            SpacerH(6.dp)
            RectangleShimmer(modifier = Modifier.size(width = 176.dp, height = 18.dp), radius = 4.dp)
            SpacerH(6.dp)
            RectangleShimmer(modifier = Modifier.size(width = 120.dp, height = 18.dp), radius = 4.dp)
            SpacerH(16.dp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                RectangleShimmer(modifier = Modifier.size(width = 72.dp, height = 24.dp), radius = 8.dp)
                RectangleShimmer(modifier = Modifier.size(width = 72.dp, height = 24.dp), radius = 8.dp)
            }
        }
    }
}