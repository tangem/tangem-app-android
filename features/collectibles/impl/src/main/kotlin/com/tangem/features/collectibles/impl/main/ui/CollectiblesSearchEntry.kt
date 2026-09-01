package com.tangem.features.collectibles.impl.main.ui

import androidx.compose.foundation.lazy.grid.LazyGridState

internal class CollectiblesSearchEntry(
    val gridState: LazyGridState,
    val isHeaderShown: Boolean,
    val isTabBandShown: Boolean,
)

internal fun createSearchEntry(mainGridState: LazyGridState, hasTabs: Boolean): CollectiblesSearchEntry {
    val leadingCount = if (hasTabs) 2 else 1
    val skipped = mainGridState.firstVisibleItemIndex.coerceAtMost(maximumValue = leadingCount)

    return CollectiblesSearchEntry(
        gridState = LazyGridState(
            firstVisibleItemIndex = mainGridState.firstVisibleItemIndex - skipped,
            firstVisibleItemScrollOffset = mainGridState.firstVisibleItemScrollOffset,
        ),
        isHeaderShown = skipped == 0,
        isTabBandShown = hasTabs && skipped <= 1,
    )
}