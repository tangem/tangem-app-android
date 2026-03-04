package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.HorizontalFadeWithBlur
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.extensions.conditionalCompose
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.components.articles.ArticleCard
import com.tangem.features.feed.ui.feed.components.articles.ShowMoreArticlesCard
import com.tangem.features.feed.ui.feed.state.NewsSliderConfig

@Suppress("LongMethod")
@Composable
internal fun NewsSlider(newsSliderConfig: NewsSliderConfig) {
    val background = LocalMainBottomSheetColor.current.value
    val isRedesignEnabled = LocalRedesignEnabled.current
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyRow(
            modifier = Modifier
                .conditionalCompose(
                    condition = isRedesignEnabled,
                    modifier = {
                        hazeSourceTangem(zIndex = -1f)
                    },
                )
                .background(color = background),
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
                        .width(228.dp)
                        .heightIn(min = 172.dp)
                        .fillMaxHeight(),
                    colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
                )
            }

            if (newsSliderConfig.shouldShowSeeAllNewsItem) {
                item(contentType = "show_more") {
                    ShowMoreArticlesCard(
                        modifier = Modifier
                            .width(228.dp)
                            .heightIn(min = 172.dp)
                            .onFirstVisible(
                                minFractionVisible = 0.5f,
                                callback = newsSliderConfig.callbacks.onSliderEndReached,
                            ),
                        onClick = newsSliderConfig.callbacks.onOpenAllNews,
                    )
                }
            }
        }
        if (isRedesignEnabled) {
            HorizontalFadeWithBlur(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .heightIn(min = 172.dp)
                    .fillMaxHeight()
                    .width(100.dp),
                backgroundColor = background,
            )
        }
    }
}