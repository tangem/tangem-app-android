package com.tangem.features.feed.ui.feed.components.articles

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.LocalRedesignEnabled

@Composable
fun ShowMoreArticlesCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isRedesignEnabled: Boolean = LocalRedesignEnabled.current
    if (isRedesignEnabled) {
        ShowMoreArticlesCardV2(modifier = modifier, onClick = onClick)
    } else {
        ShowMoreArticlesCardV1(modifier = modifier, onClick = onClick)
    }
}