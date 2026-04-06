package com.tangem.features.feed.ui.feed.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.markets.MarketsListItemPlaceholder
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
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
    if (LocalRedesignEnabled.current) {
        RectangleShimmer(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .size(width = 132.dp, height = 24.dp),
            radius = TangemTheme.dimens2.x25,
        )
        SpacerH(20.dp)
        ChartsLoading(modifier = Modifier.padding(horizontal = 16.dp))
        SpacerH(40.dp)
    } else {
        RectangleShimmer(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(width = 104.dp, height = 18.dp),
        )
        SpacerH(15.dp)
        ChartsLoading(modifier = Modifier.padding(horizontal = 16.dp))
        SpacerH(35.dp)
    }
}

@Composable
internal fun MarketPulseLoadingBlock() {
    if (LocalRedesignEnabled.current) {
        RectangleShimmer(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .size(width = 132.dp, height = 24.dp),
            radius = TangemTheme.dimens2.x25,
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
                RectangleShimmer(
                    modifier = Modifier.size(width = 112.dp, height = 36.dp),
                    radius = TangemTheme.dimens2.x25,
                )
            }
        }
        SpacerH(16.dp)
        ChartsLoading(modifier = Modifier.padding(horizontal = 16.dp))
        SpacerH(32.dp)
    } else {
        RectangleShimmer(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(width = 104.dp, height = 18.dp),
        )
        SpacerH(15.dp)
        LazyRow(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            state = rememberLazyListState(),
        ) {
            items(DEFAULT_CHART_SIZE_IN_MARKET) {
                RectangleShimmer(modifier = Modifier.size(width = 124.dp, height = 36.dp))
            }
        }
        SpacerH(16.dp)
        ChartsLoading(modifier = Modifier.padding(horizontal = 16.dp))
        SpacerH(32.dp)
    }
}

@Composable
internal fun NewsLoadingBlock() {
    if (LocalRedesignEnabled.current) {
        Column {
            RectangleShimmer(
                modifier = Modifier
                    .padding(start = 16.dp, top = 16.dp)
                    .size(width = 132.dp, height = 24.dp),
                radius = TangemTheme.dimens2.x25,
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
    } else {
        Column {
            RectangleShimmer(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(width = 104.dp, height = 18.dp),
            )
            SpacerH(15.dp)
            TrendingLoadingArticle(modifier = Modifier.padding(horizontal = 16.dp))
            SpacerH(12.dp)
            LazyRow(
                verticalAlignment = Alignment.CenterVertically,
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                state = rememberLazyListState(),
            ) {
                items(DEFAULT_CHART_SIZE_IN_MARKET) {
                    DefaultLoadingArticle()
                }
            }
            SpacerH(35.dp)
        }
    }
}

@Composable
private fun ChartsLoading(modifier: Modifier = Modifier) {
    val isRedesignEnabled = LocalRedesignEnabled.current
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(
            containerColor = if (isRedesignEnabled) {
                TangemTheme.colors2.surface.level3
            } else {
                TangemTheme.colors.background.action
            },

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
    TangemThemePreview {
        Column {
            MarketLoadingBlock()
            NewsLoadingBlock()
            MarketPulseLoadingBlock()
        }
    }
}