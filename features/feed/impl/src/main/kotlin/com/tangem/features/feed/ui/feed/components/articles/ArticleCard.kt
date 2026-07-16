package com.tangem.features.feed.ui.feed.components.articles

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ArticleCard(articleConfigUM: ArticleConfigUM, onArticleClick: () -> Unit, modifier: Modifier = Modifier) {
    ArticleCardV2(
        articleConfigUM = articleConfigUM,
        onArticleClick = onArticleClick,
        modifier = modifier,
    )
}