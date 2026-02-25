package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onFirstVisible
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.tangem.features.feed.ui.feed.components.articles.ArticleCard
import com.tangem.features.feed.ui.feed.components.articles.ShowMoreArticlesCard
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.ui.feed.state.NewsSliderConfig

private val dividerSpacerWidth = 20.dp

@Suppress("MagicNumber", "LongMethod")
@Composable
internal fun NewsSliderV2(newsSliderConfig: NewsSliderConfig) {
    val density = LocalDensity.current
    val dividerWidthPx = with(density) { 1.dp.roundToPx() }
    val spacerWidthPx = with(density) { dividerSpacerWidth.roundToPx() }

    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 16.dp),
        state = rememberLazyListState(),
    ) {
        itemsIndexed(
            items = newsSliderConfig.content,
            key = { index, _ -> index },
            contentType = { _, _ -> "article" },
        ) { index, article ->
            val articleModifier = Modifier.conditional(
                condition = index == FOURTH_ITEM_INDEX,
                modifier = {
                    onFirstVisible(
                        minFractionVisible = 0.5f,
                        callback = newsSliderConfig.callbacks.onSliderScroll,
                    )
                },
            )

            val shouldShowDivider = newsSliderConfig.shouldShowSeeAllNewsItem ||
                index < newsSliderConfig.content.size - 1

            // have to use layout cause LazyRow has not fixed height and divider can not be measured
            Layout(
                modifier = Modifier,
                content = {
                    ArticleCard(
                        articleConfigUM = article,
                        onArticleClick = { newsSliderConfig.callbacks.onArticleClick(article.id) },
                        modifier = articleModifier
                            .fillMaxHeight()
                            .width(220.dp),
                    )
                    Spacer(modifier = Modifier.width(dividerSpacerWidth))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .background(TangemTheme.colors2.border.neutral.secondary),
                    )
                    Spacer(modifier = Modifier.width(dividerSpacerWidth))
                },
            ) { measurables, constraints ->
                val cardPlaceable = measurables[0].measure(constraints)
                val height = cardPlaceable.height

                if (shouldShowDivider) {
                    val leftSpacer = measurables[1].measure(Constraints.fixed(spacerWidthPx, height))
                    val divider = measurables[2].measure(Constraints.fixed(dividerWidthPx, height))
                    val rightSpacer = measurables[3].measure(Constraints.fixed(spacerWidthPx, height))
                    val totalWidth = cardPlaceable.width + leftSpacer.width + divider.width + rightSpacer.width

                    layout(totalWidth, height) {
                        cardPlaceable.place(0, 0)
                        leftSpacer.place(cardPlaceable.width, 0)
                        divider.place(cardPlaceable.width + leftSpacer.width, 0)
                        rightSpacer.place(cardPlaceable.width + leftSpacer.width + divider.width, 0)
                    }
                } else {
                    layout(cardPlaceable.width, height) {
                        cardPlaceable.place(0, 0)
                    }
                }
            }
        }

        if (newsSliderConfig.shouldShowSeeAllNewsItem) {
            item(contentType = "show_more") {
                ShowMoreArticlesCard(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(216.dp)
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