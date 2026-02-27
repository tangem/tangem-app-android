package com.tangem.features.feed.ui.feed.components.articles

import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.res.LocalRedesignEnabled

@Composable
fun ArticleCard(
    articleConfigUM: ArticleConfigUM,
    onArticleClick: () -> Unit,
    modifier: Modifier = Modifier,
    colors: CardColors = TangemBlockCardColors,
) {
    val isRedesignEnabled = LocalRedesignEnabled.current

    if (isRedesignEnabled) {
        ArticleCardV2(
            articleConfigUM = articleConfigUM,
            onArticleClick = onArticleClick,
            modifier = modifier,
        )
    } else {
        ArticleCardV1(
            articleConfigUM = articleConfigUM,
            onArticleClick = onArticleClick,
            modifier = modifier,
            colors = colors,
        )
    }
}