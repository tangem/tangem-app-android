package com.tangem.features.feed.rwa.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.charts.MarketChartMini
import com.tangem.common.ui.markets.tokenselector.tokenGroupRowDecoration
import com.tangem.core.ui.ds2.tokenrow.Shimmer
import com.tangem.core.ui.ds2.tokenrow.TangemTokenRowMarket
import com.tangem.features.feed.rwa.ui.state.RwaListItemUM
import kotlinx.collections.immutable.ImmutableList

internal const val TOKEN_KEY_PREFIX = "token:"
internal const val SKELETON_ROW_COUNT = 20

/** Inset of the row content inside whatever the caller pads and decorates. Applied by the row and its skeleton alike. */
private val ROW_INNER_PADDING = 4.dp

/**
 * Real-asset rows, shared by the feed tab's list and the search tab's results section.
 *
 * @param horizontalPadding inset of each row. The feed tab pads per row; the search tab insets its whole
 * list instead and passes nothing.
 * @param hasGroupedBackground rounded block treatment, so search results read like the portfolio sections
 * they sit under
 */
internal fun LazyListScope.rwaTokenItems(
    items: ImmutableList<RwaListItemUM>,
    horizontalPadding: Dp = 0.dp,
    hasGroupedBackground: Boolean = false,
) {
    itemsIndexed(items = items, key = { _, item -> TOKEN_KEY_PREFIX + item.id }) { index, item ->
        RwaListRow(
            item = item,
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .groupedBackground(enabled = hasGroupedBackground, index = index, lastIndex = items.lastIndex),
        )
    }
}

internal fun LazyListScope.rwaTokenShimmers(
    count: Int = SKELETON_ROW_COUNT,
    horizontalPadding: Dp = 0.dp,
    hasGroupedBackground: Boolean = false,
) {
    items(count = count, key = { "skeleton_$it" }) { index ->
        TangemTokenRowMarket.Shimmer(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .groupedBackground(enabled = hasGroupedBackground, index = index, lastIndex = count - 1)
                .padding(horizontal = ROW_INNER_PADDING),
        )
    }
}

@Composable
private fun Modifier.groupedBackground(enabled: Boolean, index: Int, lastIndex: Int): Modifier =
    if (enabled) tokenGroupRowDecoration(currentIndex = index, lastIndex = lastIndex) else this

/**
 * Reports the token ids currently on screen, so the model can fetch charts for their batches.
 *
 * The search tab does not use this: its charts are loaded once per batch from
 * `onLastBatchLoadedSuccess`, matching the Crypto search tab.
 */
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
private fun RwaListRow(item: RwaListItemUM, modifier: Modifier = Modifier) {
    TangemTokenRowMarket(
        state = item.row,
        modifier = modifier.padding(horizontal = ROW_INNER_PADDING),
        chart = {
            item.chartData?.let { chartData ->
                MarketChartMini(
                    rawData = chartData,
                    type = item.chartType,
                    modifier = Modifier.size(width = 24.dp, height = 32.dp),
                )
            }
        },
    )
}