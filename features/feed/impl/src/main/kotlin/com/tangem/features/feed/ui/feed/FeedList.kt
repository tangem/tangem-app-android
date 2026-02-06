package com.tangem.features.feed.ui.feed

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.extensions.conditional
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.BaseSearchBarTestTags.SEARCH_BAR
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
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
    val background = LocalMainBottomSheetColor.current.value
    FeedSearchBar(
        isSearchBarClickable = isSearchBarClickable,
        feedListSearchBar = feedListSearchBar,
        modifier = modifier
            .drawBehind { drawRect(background) }
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp)
            .testTag(SEARCH_BAR),
    )
}

@Composable
internal fun FeedList(state: FeedListUM, modifier: Modifier = Modifier) {
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
                        .verticalScroll(rememberScrollState())
                        .drawBehind { drawRect(background) },
                )
            }
            is GlobalFeedState.Error -> {
                FeedListGlobalError(
                    onRetryClick = animatedState.onRetryClicked,
                    modifier = Modifier.drawBehind { drawRect(background) },
                    currentDate = state.currentDate,
                )
            }
            is GlobalFeedState.Content -> {
                FeedListContent(
                    modifier = Modifier,
                    state = state,
                )
            }
        }
    }
}

@Composable
private fun FeedSearchBar(
    isSearchBarClickable: Boolean,
    feedListSearchBar: FeedListSearchBar,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(36.dp))
            .background(color = TangemTheme.colors.field.focused)
            .conditional(condition = isSearchBarClickable) {
                clickable(onClick = feedListSearchBar.onBarClick)
            }
            .padding(14.dp),
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size20),
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_search_24),
            tint = TangemTheme.colors.icon.informative,
            contentDescription = null,
        )

        SpacerW(14.dp)

        Text(
            text = feedListSearchBar.placeholderText.resolveReference(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun FeedListContent(state: FeedListUM, modifier: Modifier = Modifier) {
    val background = LocalMainBottomSheetColor.current.value
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
            .drawBehind { drawRect(background) },
    ) {
        DateBlock(state.currentDate)
        SpacerH(32.dp)

        MarketBlock(
            marketChart = state.marketChartConfig.marketCharts[SortByTypeUM.Rating],
            feedListCallbacks = state.feedListCallbacks,
        )

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
        FeedList(state = createFeedPreviewState())
    }
}