package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.features.collectibles.impl.discovery.ui.DiscoveryListKeys

internal data class CollectiblesListFill(val statusHeight: Dp, val fillerHeight: Dp)

@Composable
internal fun rememberListFill(listState: LazyListState, rowCount: Int, hasTabs: Boolean): CollectiblesListFill {
    val density = LocalDensity.current
    val bandBlockHeightPx = with(density) { 64.dp.roundToPx() }

    val measuredRowHeight by remember(listState) {
        derivedStateOf {
            listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.contentType == DiscoveryListKeys.CONTENT_TYPE_PACK_ROW }
                ?.size
                ?: 0
        }
    }
    // Kept across the swap of list states: a row measures the same in both, and the search list has no layout of
    // its own on its first frame — recomputing there would blow the filler up to a whole viewport.
    var knownRowHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(measuredRowHeight) {
        if (measuredRowHeight > 0) knownRowHeight = measuredRowHeight
    }

    val metrics by remember(listState, rowCount, hasTabs, bandBlockHeightPx, knownRowHeight) {
        derivedStateOf {
            val info = listState.layoutInfo
            val available = (info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding)
                .coerceAtLeast(minimumValue = 0)

            // Measured, not assumed: the leading blocks shrink to nothing while folding into search, and a
            // status box sized past them would centre its content off by half of whatever it overshot.
            val leadingHeight = info.visibleItemsInfo.sumOf { item ->
                if (item.key == MainListKeys.HEADER || item.key == DiscoveryListKeys.TABS) item.size else 0
            }

            // What is left of the viewport below the tab band once it is pinned, top bar and bottom inset
            // excluded. Filling exactly this leaves the header as the only thing that scrolls away.
            val bandHeight = if (hasTabs) bandBlockHeightPx else 0
            val filler = (available - bandHeight - rowCount * knownRowHeight).coerceAtLeast(minimumValue = 0)

            (available - leadingHeight).coerceAtLeast(minimumValue = 0) to filler
        }
    }
    return with(density) {
        CollectiblesListFill(statusHeight = metrics.first.toDp(), fillerHeight = metrics.second.toDp())
    }
}