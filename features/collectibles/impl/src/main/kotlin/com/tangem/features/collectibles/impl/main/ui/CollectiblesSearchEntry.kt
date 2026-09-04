package com.tangem.features.collectibles.impl.main.ui

import androidx.compose.foundation.lazy.LazyListState

internal class CollectiblesSearchEntry(
    val listState: LazyListState,
    val isHeaderShown: Boolean,
    val isTabBandShown: Boolean,
)

internal fun createSearchEntry(mainListState: LazyListState, hasTabs: Boolean): CollectiblesSearchEntry {
    val leadingCount = if (hasTabs) 2 else 1
    val skipped = mainListState.firstVisibleItemIndex.coerceAtMost(maximumValue = leadingCount)

    return CollectiblesSearchEntry(
        listState = LazyListState(
            firstVisibleItemIndex = mainListState.firstVisibleItemIndex,
            firstVisibleItemScrollOffset = mainListState.firstVisibleItemScrollOffset,
        ),
        isHeaderShown = skipped == 0,
        isTabBandShown = hasTabs && skipped <= 1,
    )
}