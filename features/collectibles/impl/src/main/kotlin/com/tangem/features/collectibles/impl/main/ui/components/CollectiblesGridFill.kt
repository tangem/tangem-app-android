package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

internal data class CollectiblesGridFill(val statusHeight: Dp, val fillerHeight: Dp)

@Composable
internal fun rememberGridFill(gridState: LazyGridState, packCount: Int, hasTabs: Boolean): CollectiblesGridFill {
    val density = LocalDensity.current
    val bandBlockHeightPx = with(density) { (TabBandHeight + TabBandBottomGap).roundToPx() }

    val measuredRowHeight by remember(gridState) {
        derivedStateOf {
            gridState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.contentType == GridKeys.CONTENT_TYPE_PACK }
                ?.size
                ?.height
                ?: 0
        }
    }
    // Kept across the swap of grid states: a row measures the same in both, and the search grid has no layout of
    // its own on its first frame — recomputing there would blow the filler up to a whole viewport.
    var knownRowHeight by remember { mutableIntStateOf(0) }
    LaunchedEffect(measuredRowHeight) {
        if (measuredRowHeight > 0) knownRowHeight = measuredRowHeight
    }

    val metrics by remember(gridState, packCount, hasTabs, bandBlockHeightPx, knownRowHeight) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val available = (info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding)
                .coerceAtLeast(minimumValue = 0)

            // Measured, not assumed: the leading blocks shrink to nothing while folding into search, and a
            // status box sized past them would centre its content off by half of whatever it overshot.
            val leadingHeight = info.visibleItemsInfo.sumOf { item ->
                if (item.contentType == GridKeys.HEADER || item.contentType == GridKeys.TABS) item.size.height else 0
            }

            // What is left of the viewport below the tab band once it is pinned, top bar and bottom inset
            // excluded. Filling exactly this leaves the header as the only thing that scrolls away.
            val bandHeight = if (hasTabs) bandBlockHeightPx else 0
            val rows = (packCount + GRID_COLUMNS - 1) / GRID_COLUMNS
            val filler = (available - bandHeight - rows * knownRowHeight).coerceAtLeast(minimumValue = 0)

            (available - leadingHeight).coerceAtLeast(minimumValue = 0) to filler
        }
    }
    return with(density) {
        CollectiblesGridFill(statusHeight = metrics.first.toDp(), fillerHeight = metrics.second.toDp())
    }
}