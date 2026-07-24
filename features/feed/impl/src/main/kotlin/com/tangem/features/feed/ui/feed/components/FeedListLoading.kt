package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.MarketsListItemPlaceholder
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.feed.ui.feed.components.articles.DefaultLoadingArticle
import com.tangem.features.feed.ui.feed.components.articles.TrendingLoadingArticle

@Composable
internal fun FeedListLoading(modifier: Modifier = Modifier) {
    Column(modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.navigationBars.asPaddingValues()),
        ) {
            MarketLoadingBlock()
            NewsLoadingBlock()
            MarketPulseLoadingBlock()
        }
    }
}

@Composable
internal fun MarketLoadingBlock() {
    SectionTitleShimmer(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
    )
    SpacerH(20.dp)
    ChartsLoading(modifier = Modifier.padding(horizontal = 16.dp))
    SpacerH(40.dp)
}

@Composable
internal fun MarketPulseLoadingBlock() {
    SectionTitleShimmer(
        modifier = Modifier.padding(start = 16.dp, top = 16.dp),
    )
    SpacerH(15.dp)
    LazyRow(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        state = rememberLazyListState(),
    ) {
        items(DEFAULT_CHART_SIZE_IN_MARKET) {
            FilterTabShimmer()
        }
    }
    SpacerH(16.dp)
    ChartsLoading(modifier = Modifier.padding(horizontal = 16.dp))
    SpacerH(32.dp)
}

@Composable
internal fun NewsLoadingBlock() {
    Column {
        SectionTitleShimmer(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
        )
        SpacerH(20.dp)
        TrendingLoadingArticle(modifier = Modifier.padding(horizontal = 16.dp))
        SpacerH(12.dp)
        LazyRow(
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            state = rememberLazyListState(),
        ) {
            items(DEFAULT_CHART_SIZE_IN_MARKET) {
                DefaultLoadingArticle()
            }
        }
        SpacerH(40.dp)
    }
}

@Composable
private fun SectionTitleShimmer(modifier: Modifier = Modifier) {
    val lineHeight = with(LocalDensity.current) {
        TangemTheme.typography3.heading.small.lineHeight.toDp()
    }
    TangemShimmer(
        radius = 16.dp,
        modifier = modifier
            .width(132.dp)
            .height(lineHeight)
            .padding(vertical = 2.dp),
    )
}

@Composable
private fun FilterTabShimmer(modifier: Modifier = Modifier) {
    TangemShimmer(
        radius = 999.dp,
        modifier = modifier.size(width = 112.dp, height = 36.dp),
    )
}

@Composable
private fun ChartsLoading(modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(
            containerColor = TangemTheme.colors3.bg.secondary,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            repeat(DEFAULT_CHART_SIZE_IN_MARKET) {
                MarketsListItemPlaceholder()
            }
        }
    }
}

private const val DEFAULT_CHART_SIZE_IN_MARKET = 5

@Preview(showBackground = true)
@Composable
private fun FeedListLoadingPreview() {
    TangemThemePreviewRedesign {
        Column {
            MarketLoadingBlock()
            NewsLoadingBlock()
            MarketPulseLoadingBlock()
        }
    }
}