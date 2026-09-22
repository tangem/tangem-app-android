package com.tangem.features.collectibles.impl.main.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.features.collectibles.impl.discovery.ui.DiscoveryListKeys

internal data class CollectiblesListFill(
    val statusHeight: Dp,
    val fillerHeight: Dp,
    val foldedFillerHeight: Dp,
)

/**
 * The filler height to hand the list. Frozen at the resting measurement while search is being entered, so
 * the scroll range still covers the leading blocks; live afterwards, so the keyboard shrinks it instead of
 * leaving empty space to scroll through.
 *
 * Remember it where the list's call site cannot change, or the frozen value goes with the composition.
 */
@Composable
internal fun rememberFillerHeight(fill: CollectiblesListFill, isSearchActive: Boolean, isEnteringSearch: Boolean): Dp {
    var restingFoldedHeight by remember { mutableStateOf(0.dp) }

    LaunchedEffect(isSearchActive, fill.foldedFillerHeight) {
        if (!isSearchActive) restingFoldedHeight = fill.foldedFillerHeight
    }

    return when {
        !isSearchActive -> fill.fillerHeight
        isEnteringSearch -> restingFoldedHeight
        else -> fill.foldedFillerHeight
    }
}

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

            val leadingHeight = info.visibleItemsInfo.sumOf { item ->
                if (item.key == MainListKeys.HEADER || item.key == DiscoveryListKeys.TABS) item.size else 0
            }

            val rowsHeight = rowCount * knownRowHeight
            val bandHeight = if (hasTabs) bandBlockHeightPx else 0

            Metrics(
                statusHeight = (available - leadingHeight).coerceAtLeast(minimumValue = 0),
                fillerHeight = (available - bandHeight - rowsHeight).coerceAtLeast(minimumValue = 0),
                foldedFillerHeight = (available - rowsHeight).coerceAtLeast(minimumValue = 0),
            )
        }
    }
    return with(density) {
        CollectiblesListFill(
            statusHeight = metrics.statusHeight.toDp(),
            fillerHeight = metrics.fillerHeight.toDp(),
            foldedFillerHeight = metrics.foldedFillerHeight.toDp(),
        )
    }
}

private data class Metrics(val statusHeight: Int, val fillerHeight: Int, val foldedFillerHeight: Int)