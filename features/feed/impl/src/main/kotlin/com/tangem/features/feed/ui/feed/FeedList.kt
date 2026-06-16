package com.tangem.features.feed.ui.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.test.BaseSearchBarTestTags.SEARCH_BAR
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.components.FeedSearchBar
import com.tangem.features.feed.ui.feed.components.*
import com.tangem.features.feed.ui.feed.preview.FeedListPreviewDataProvider.createFeedPreviewState
import com.tangem.features.feed.ui.feed.state.FeedListSearchBar
import com.tangem.features.feed.ui.feed.state.FeedListUM
import com.tangem.features.feed.ui.feed.state.GlobalFeedState

@Composable
internal fun FeedListHeader(
    isSearchBarClickable: Boolean,
    feedListSearchBar: FeedListSearchBar,
    modifier: Modifier = Modifier,
) {
    FeedSearchBar(
        isSearchBarClickable = isSearchBarClickable,
        feedListSearchBar = feedListSearchBar,
        modifier = modifier.testTag(SEARCH_BAR),
    )
}

@Composable
internal fun FeedList(
    contentPadding: PaddingValues,
    state: FeedListUM,
    modifier: Modifier = Modifier,
    promoBannersBlockComponent: ComposableContentComponent? = null,
) {
    val background = LocalMainBottomSheetColor.current.value
    AnimatedContent(
        modifier = modifier,
        targetState = state.globalState,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { animatedState ->
        when (animatedState) {
            is GlobalFeedState.Loading -> {
                FeedListLoading(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind { drawRect(background) }
                        .padding(top = contentPadding.calculateTopPadding())
                        .verticalScroll(rememberScrollState()),
                )
            }
            is GlobalFeedState.Error -> {
                FeedListGlobalError(
                    onRetryClick = animatedState.onRetryClicked,
                    modifier = Modifier
                        .drawBehind { drawRect(background) }
                        .padding(top = contentPadding.calculateTopPadding()),
                    currentDate = state.currentDate,
                )
            }
            is GlobalFeedState.Content -> {
                FeedListContent(
                    contentPadding = contentPadding,
                    modifier = Modifier,
                    state = state,
                    promoBannersBlockComponent = promoBannersBlockComponent,
                )
            }
        }
    }
}

@Composable
private fun FeedListContent(
    contentPadding: PaddingValues,
    state: FeedListUM,
    modifier: Modifier = Modifier,
    promoBannersBlockComponent: ComposableContentComponent? = null,
) {
    val background = LocalMainBottomSheetColor.current.value
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            .drawBehind { drawRect(background) },
    ) {
        if (LocalRedesignEnabled.current) {
            SpacerH(contentPadding.calculateTopPadding())
        }
        DateBlock(state.currentDate)
        SpacerH(32.dp)

        MarketBlock(
            marketChart = state.marketChartConfig.marketCharts[SortByTypeUM.Rating],
            onSeeAllClick = { state.feedListCallbacks.onMarketOpenClick(SortByTypeUM.Rating) },
            onItemClick = state.feedListCallbacks.onMarketItemClick,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        promoBannersBlockComponent?.Content(
            modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
        )
        SpacerH(32.dp)

        NewsBlock(
            news = state.news,
            feedListCallbacks = state.feedListCallbacks,
            trendingArticle = state.trendingArticle,
        )

        EarnBlock(
            onSeeAllClick = state.feedListCallbacks.onOpenEarnPageClick,
            earnListUM = state.earnListUM,
        )

        MarketPulseBlock(
            marketChartConfig = state.marketChartConfig,
            feedListCallbacks = state.feedListCallbacks,
        )
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun FeedListPreview() {
    TangemThemePreview {
        FeedList(state = createFeedPreviewState(), contentPadding = PaddingValues())
    }
}

@Preview(showBackground = true, heightDp = 1500)
@Composable
private fun FeedListPreviewV2() {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            FeedList(state = createFeedPreviewState(), contentPadding = PaddingValues())
        }
    }
}