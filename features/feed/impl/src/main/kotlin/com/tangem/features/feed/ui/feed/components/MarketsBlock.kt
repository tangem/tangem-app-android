package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.markets.MarketsListItem
import com.tangem.common.ui.markets.MarketsListItemPlaceholder
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.UnableToLoadData
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.block.TangemBlockCardColors
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.ui.feed.state.FeedListCallbacks
import com.tangem.features.feed.ui.feed.state.MarketChartConfig
import com.tangem.features.feed.ui.feed.state.MarketChartUM

@Composable
internal fun MarketBlock(marketChart: MarketChartUM?, feedListCallbacks: FeedListCallbacks) {
    AnimatedContent(
        targetState = marketChart,
        label = "MarketBlockChartAnimation",
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { currentChart ->
        when (currentChart) {
            is MarketChartUM.Content,
            MarketChartUM.Loading,
            is MarketChartUM.LoadingError,
            -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Header(
                        title = {
                            Text(
                                text = stringResourceSafe(R.string.markets_common_title),
                                style = TangemTheme.typography.h3,
                                color = TangemTheme.colors.text.primary1,
                            )
                        },
                        onSeeAllClick = { feedListCallbacks.onMarketOpenClick(SortByTypeUM.Rating) },
                    )

                    SpacerH(12.dp)

                    Charts(
                        onItemClick = feedListCallbacks.onMarketItemClick,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        marketChart = currentChart,
                    )

                    SpacerH(32.dp)
                }
            }
            null -> Unit
        }
    }
}

@Composable
internal fun MarketPulseBlock(marketChartConfig: MarketChartConfig, feedListCallbacks: FeedListCallbacks) {
    val onSeeAllClick by rememberUpdatedState {
        feedListCallbacks.onMarketOpenClick(marketChartConfig.currentSortByType)
    }
    if (marketChartConfig.marketCharts.isNotEmpty()) {
        Header(
            title = {
                Text(
                    text = stringResourceSafe(R.string.markets_pulse_common_title),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                )
            },
            onSeeAllClick = { onSeeAllClick() },
        )

        LazyRow(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            state = rememberLazyListState(),
        ) {
            items(
                items = marketChartConfig.getFilterPreset(),
                key = SortByTypeUM::name,
            ) { sortByTypeUM ->
                ChartsFilterChip(
                    sortByTypeUM = sortByTypeUM,
                    isSelected = sortByTypeUM == marketChartConfig.currentSortByType,
                    onClick = { feedListCallbacks.onSortTypeClick(sortByTypeUM) },
                )
            }
        }

        SpacerH(12.dp)

        AnimatedContent(
            targetState = marketChartConfig.currentSortByType,
            label = "MarketPulseChartAnimation",
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { currentSortType ->
            marketChartConfig.marketCharts[currentSortType]?.let { chart ->
                Charts(
                    onItemClick = feedListCallbacks.onMarketItemClick,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    marketChart = chart,
                )
            }
        }
        SpacerH(32.dp)
    }
}

@Composable
private fun Charts(
    marketChart: MarketChartUM,
    onItemClick: (MarketsListItemUM) -> Unit,
    modifier: Modifier = Modifier,
) {
    BlockCard(
        modifier = modifier,
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors.background.action),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            when (marketChart) {
                MarketChartUM.Loading -> {
                    repeat(DEFAULT_CHART_SIZE_IN_MARKET) {
                        MarketsListItemPlaceholder()
                    }
                }
                is MarketChartUM.LoadingError -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 35.dp, horizontal = 10.dp),
                    ) {
                        UnableToLoadData(
                            onRetryClick = marketChart.onRetryClicked,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                is MarketChartUM.Content -> {
                    marketChart.items.fastForEach { chart ->
                        MarketsListItem(
                            model = chart,
                            onClick = { onItemClick(chart) },
                        )
                    }
                }
            }
        }
    }
}

private const val DEFAULT_CHART_SIZE_IN_MARKET = 5