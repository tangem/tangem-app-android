package com.tangem.features.feed.ui.news.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.TopFade
import com.tangem.core.ui.components.chip.Chip
import com.tangem.core.ui.components.chip.entity.ChipUM
import com.tangem.core.ui.components.haze.hazeSourceTangem
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.ds.tabs.TangemTab
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.feed.ui.LocalContentTopFadeHeightOverride
import com.tangem.features.feed.ui.feed.components.articles.ArticleConfigUM
import com.tangem.features.feed.ui.news.list.components.NewsListLazyColumn
import com.tangem.features.feed.ui.news.list.state.NewsListState
import com.tangem.features.feed.ui.news.list.state.NewsListUM
import com.tangem.features.feed.ui.utils.FadeConstants.BASE_FADE_LEVEL
import com.tangem.features.feed.ui.utils.FadeConstants.FIRST_STEP
import com.tangem.features.feed.ui.utils.FadeConstants.FIRST_STEP_FADE_LEVEL
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
    val chipsListState = rememberLazyListState()

    ScrollChipsToSelected(state = state, chipsListState = chipsListState)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(background),
    ) {
        SpacerH(contentPadding.calculateTopPadding())
        LazyRow(
            state = chipsListState,
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
    val chipsListState = rememberLazyListState()
    var chipsHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    val fadeColor = TangemTheme.colors2.surface.level2.copy(BASE_FADE_LEVEL)
    val topPadding = contentPadding.calculateTopPadding()

    val centralFadeOverride = LocalContentTopFadeHeightOverride.current
    DisposableEffect(centralFadeOverride) {
        centralFadeOverride?.value = 0.dp
        onDispose { centralFadeOverride?.value = null }
    }

    ScrollChipsToSelected(state = state, chipsListState = chipsListState)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
    ) {
        NewsListLazyColumn(
            topContentPadding = contentPadding.calculateTopPadding() + 16.dp + chipsHeight,
            modifier = Modifier
                .align(Alignment.TopStart)
                .hazeSourceTangem(zIndex = 0f),
            newsListState = state.newsListState,
            listOfArticles = state.listOfArticles,
            lazyListState = lazyListState,
            onArticleClick = state.onArticleClick,
        )

        TopFade(
            modifier = Modifier.padding(top = contentPadding.calculateTopPadding()),
            colorStops = arrayOf(
                0f to fadeColor,
                FIRST_STEP to fadeColor.copy(FIRST_STEP_FADE_LEVEL),
                1f to Color.Transparent,
            ),
            height = 20.dp + chipsHeight,
        )

        LazyRow(
            state = chipsListState,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topPadding, bottom = TangemTheme.dimens2.x4)
                .onGloballyPositioned { coordinates ->
                    if (coordinates.size.height > 0) {
                        with(density) {
                            chipsHeight = coordinates.size.height.toDp()
                        }
                    }
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

@Composable
private fun ScrollChipsToSelected(state: NewsListUM, chipsListState: LazyListState) {
    EventEffect(event = state.scrollToCategoryEvent) { index ->
        chipsListState.animateScrollToItem(index)
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