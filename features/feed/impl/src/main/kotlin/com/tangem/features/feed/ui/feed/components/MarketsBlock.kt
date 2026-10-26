package com.tangem.features.feed.ui.feed.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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
import com.tangem.features.feed.ui.feed.state.MarketChartUM

@Composable
internal fun MarketBlock(
    marketChart: MarketChartUM?,
    onSeeAllClick: () -> Unit,
    onItemClick: (MarketsListItemUM) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Column(modifier = modifier.fillMaxWidth()) {
                    Header(
                        title = {
                            Text(
                                text = stringResourceSafe(R.string.markets_common_title),
                                style = TangemTheme.typography3.heading.small,
                                color = TangemTheme.colors3.text.primary,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                            )
                        },
                        onSeeAllClick = onSeeAllClick,
                        shouldShowSeeAll = currentChart is MarketChartUM.Content,
                        isLoading = currentChart is MarketChartUM.Loading,
                        // Header text is inset so it aligns with the list item text inside the card; the card itself
                        // sits at the block edge (supplied by [modifier]).
                        modifier = Modifier.padding(horizontal = MARKET_BLOCK_HEADER_PADDING),
                    )

                    SpacerH(20.dp)

                    Charts(
                        onItemClick = onItemClick,
                        marketChart = currentChart,
                    )
                }
            }
            null -> Unit
        }
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
        colors = TangemBlockCardColors.copy(containerColor = TangemTheme.colors3.bg.secondary),
        shape = RoundedCornerShape(24.dp),
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

/** Header inset so its text aligns with the list item text inside the card (the card sits at the block edge). */
private val MARKET_BLOCK_HEADER_PADDING = 4.dp