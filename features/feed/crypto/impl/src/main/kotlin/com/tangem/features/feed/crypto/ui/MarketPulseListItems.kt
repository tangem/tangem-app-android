package com.tangem.features.feed.crypto.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.markets.tokenselector.tokenGroupRowDecoration
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.tokenrow.Shimmer
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.features.feed.crypto.ui.state.MarketPulseItemUM
import kotlinx.collections.immutable.ImmutableList

internal const val TOKEN_KEY_PREFIX = "token:"
internal const val LOADING_PLACEHOLDER_COUNT = 20

/**
 * Market rows, shared by the feed tab's Market Pulse list and the search tab's market section.
 *
 * @param horizontalPadding inset of each row. The feed tab pads per row; the search tab insets its whole
 * list instead and passes nothing.
 */
internal fun LazyListScope.marketTokenItems(
    items: ImmutableList<MarketPulseItemUM>,
    horizontalPadding: Dp = 0.dp,
    hasGroupedBackground: Boolean = false,
) {
    itemsIndexed(items = items, key = { _, item -> TOKEN_KEY_PREFIX + item.id }) { index, item ->
        MarketPulseItem(
            item = item,
            modifier = Modifier
                .animateItem()
                .padding(horizontal = horizontalPadding)
                .groupedBackground(enabled = hasGroupedBackground, index = index, lastIndex = items.lastIndex),
        )
    }
}

internal fun LazyListScope.marketTokenShimmers(
    count: Int = LOADING_PLACEHOLDER_COUNT,
    horizontalPadding: Dp = 0.dp,
    hasGroupedBackground: Boolean = false,
) {
    items(count = count, key = { "placeholder:$it" }) { index ->
        TangemTokenRowMarket.Shimmer(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .groupedBackground(enabled = hasGroupedBackground, index = index, lastIndex = count - 1),
        )
    }
}

/** Same rounded block treatment the portfolio sections use, so both blocks read alike. */
@Composable
private fun Modifier.groupedBackground(enabled: Boolean, index: Int, lastIndex: Int): Modifier =
    if (enabled) tokenGroupRowDecoration(currentIndex = index, lastIndex = lastIndex) else this

/** Reports the token ids currently on screen, so the model can fetch charts for their batches. */
@Composable
internal fun VisibleTokensTracker(
    listState: LazyListState,
    isTrackingEnabled: Boolean,
    onVisibleItemsChange: (List<String>) -> Unit,
) {
    val visibleItems by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo.mapNotNull { itemInfo ->
                (itemInfo.key as? String)
                    ?.takeIf { it.startsWith(TOKEN_KEY_PREFIX) }
                    ?.removePrefix(TOKEN_KEY_PREFIX)
            }
        }
    }

    LaunchedEffect(listState.isScrollInProgress, visibleItems, isTrackingEnabled) {
        onVisibleItemsChange(if (isTrackingEnabled) visibleItems else emptyList())
    }
}

@Composable
private fun MarketPulseItem(item: MarketPulseItemUM, modifier: Modifier = Modifier) {
    TangemTokenRowMarket(
        state = item.row,
        modifier = modifier,
        chart = {
            val chartData = item.chartData
            if (chartData != null) {
                MarketChartMini(
                    rawData = chartData,
                    type = item.chartType,
                    modifier = Modifier.size(width = 24.dp, height = 32.dp),
                )
            } else {
                ChartShimmer()
            }
        },
    )
}

@Composable
private fun ChartShimmer(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(width = 24.dp, height = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        TangemShimmer(
            radius = 4.dp,
            modifier = Modifier.size(width = 24.dp, height = 12.dp),
        )
    }
}