package com.tangem.features.feed.ui.news.list

import androidx.compose.animation.core.EaseOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.chip.Chip
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.core.ui.components.haze.hazeEffectTangem
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.ds.tabs.TangemTab
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.*
import com.tangem.features.feed.ui.feed.components.articles.ArticleConfigUM
import com.tangem.features.feed.ui.news.list.components.NewsListLazyColumn
import com.tangem.features.feed.ui.news.list.state.NewsListState
import com.tangem.features.feed.ui.news.list.state.NewsListUM
import dev.chrisbanes.haze.HazeProgressive
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableSet

@Composable
internal fun NewsListContent(contentPadding: PaddingValues, state: NewsListUM, modifier: Modifier = Modifier) {
    if (LocalRedesignEnabled.current) {
        NewsListContentV2(
            contentPadding = contentPadding,
            state = state,
        )
    } else {
        NewsListContentV1(
            contentPadding = contentPadding,
            state = state,
            modifier = modifier,
        )
    }
}

@Composable
internal fun NewsListContentV1(contentPadding: PaddingValues, state: NewsListUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    val lazyListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        SpacerH(contentPadding.calculateTopPadding())
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = state.filters,
                key = { it.id },
            ) { filter ->
                Chip(state = filter)
            }
        }

        SpacerH(16.dp)

        NewsListLazyColumn(
            newsListState = state.newsListState,
            listOfArticles = state.listOfArticles,
            lazyListState = lazyListState,
            onArticleClick = state.onArticleClick,
        )
    }
}

@Composable
internal fun NewsListContentV2(contentPadding: PaddingValues, state: NewsListUM) {
    val background = LocalMainBottomSheetColor.current.value
    val lazyListState = rememberLazyListState()
    var chipsHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        NewsListLazyColumn(
            topContentPadding = contentPadding.calculateTopPadding() + 16.dp + chipsHeight,
            modifier = Modifier
                .hazeSourceTangem(zIndex = 0f)
                .align(Alignment.TopStart),
            newsListState = state.newsListState,
            listOfArticles = state.listOfArticles,
            lazyListState = lazyListState,
            onArticleClick = state.onArticleClick,
        )
        LazyRow(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = contentPadding.calculateTopPadding(), bottom = TangemTheme.dimens2.x4)
                .onGloballyPositioned { coordinates ->
                    if (coordinates.size.height > 0) {
                        with(density) {
                            chipsHeight = coordinates.size.height.toDp()
                        }
                    }
                }
                .hazeEffectTangem {
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = .2f,
                        endIntensity = 0f,
                        easing = EaseOut,
                        preferPerformance = true,
                    )
                    backgroundColor = background
                },
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = state.filters,
                key = { it.id },
            ) { filter ->
                TangemTab(
                    text = filter.text,
                    isChecked = filter.isSelected,
                    onCheckedChange = {
                        filter.onClick()
                    },
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Preview(showBackground = true)
@Composable
private fun NewsListContentPreview() {
    val tags = listOf(
        LabelUM(TextReference.Str("Regulation")),
        LabelUM(TextReference.Str("BTC")),
        LabelUM(TextReference.Str("ETH")),
    ).toImmutableSet()

    val articles = persistentListOf(
        ArticleConfigUM(
            id = 1,
            title = "SEC delays decisions on ETH-staking ETFs and spot XRP/SOL funds",
            score = 6.5f,
            createdAt = TextReference.Str("1h ago"),
            isTrending = false,
            tags = tags,
            isViewed = false,
        ),
        ArticleConfigUM(
            id = 2,
            title = "Bitcoin ETFs log 4th straight day of inflows (+\$550M)",
            score = 8.6f,
            createdAt = TextReference.Str("8h ago"),
            isTrending = false,
            tags = tags,
            isViewed = true,
        ),
        ArticleConfigUM(
            id = 3,
            title = "Bitcoin reclaims ~\$115K amid macro prints and ETF optimism",
            score = 7.8f,
            createdAt = TextReference.Str("22 Jun, 11:30"),
            isTrending = false,
            tags = tags,
            isViewed = false,
        ),
    )

    val filters = persistentListOf(
        ChipUM(
            id = 0,
            text = TextReference.Str("All News"),
            isSelected = true,
            onClick = {},
        ),
        ChipUM(
            id = 1,
            text = TextReference.Str("Regulation"),
            isSelected = false,
            onClick = {},
        ),
        ChipUM(
            id = 2,
            text = TextReference.Str("ETFs"),
            isSelected = false,
            onClick = {},
        ),
        ChipUM(
            id = 3,
            text = TextReference.Str("Institutions"),
            isSelected = false,
            onClick = {},
        ),
    )

    TangemThemePreview {
        NewsListContent(
            state = NewsListUM(
                selectedCategoryId = 0,
                filters = filters,
                listOfArticles = articles,
                newsListState = NewsListState.Content(loadMore = {}),
                onArticleClick = {},
                onBackClick = {},
            ),
            contentPadding = PaddingValues(),
        )
    }
}