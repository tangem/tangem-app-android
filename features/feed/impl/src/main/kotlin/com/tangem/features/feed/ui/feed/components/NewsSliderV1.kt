package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.unit.dp
import com.tangem.features.feed.ui.feed.components.articles.ArticleCard
import com.tangem.features.feed.ui.feed.components.articles.ShowMoreArticlesCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.state.NewsSliderConfig

@Composable
internal fun NewsSliderV1(newsSliderConfig: NewsSliderConfig) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        state = rememberLazyListState(),
    ) {
        itemsIndexed(
            items = newsSliderConfig.content,
            key = { index, _ -> index },
            contentType = { _, _ -> "article" },
        ) { index, article ->
            val articleModifier = if (index == FOURTH_ITEM_INDEX) {
                Modifier.onFirstVisible(
                    minFractionVisible = 0.5f,
                    callback = newsSliderConfig.callbacks.onSliderScroll,
                )
            } else {
                Modifier
            }
            ArticleCard(
                articleConfigUM = article,
                onArticleClick = { newsSliderConfig.callbacks.onArticleClick(article.id) },
                modifier = articleModifier
                    .heightIn(min = 164.dp)
                    .width(216.dp),
                colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
            )
        }

        if (newsSliderConfig.shouldShowSeeAllNewsItem) {
            item(contentType = "show_more") {
                ShowMoreArticlesCard(
                    modifier = Modifier
                        .width(216.dp)
                        .heightIn(min = 164.dp)
                        .onFirstVisible(
                            minFractionVisible = 0.5f,
                            callback = newsSliderConfig.callbacks.onSliderEndReached,
                        ),
                    onClick = newsSliderConfig.callbacks.onOpenAllNews,
                )
            }
        }
    }
}