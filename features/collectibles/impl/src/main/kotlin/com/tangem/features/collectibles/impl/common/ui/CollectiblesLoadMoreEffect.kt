package com.tangem.features.collectibles.impl.common.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/** How many items before the end of the list the next page starts loading. */
private const val LOAD_MORE_THRESHOLD = 4

/**
 * Asks for the next page once the list is scrolled within [LOAD_MORE_THRESHOLD] items of its end. The model
 * decides whether there is anything left to load, so this only has to fire on approach.
 *
 * @param isEnabled `false` while the catalogue has no pages on screen. A reloading list is short enough for
 *   the threshold to hold trivially, so without this the trigger would stay latched at `true` across the
 *   reload and never fire again for the category that replaced it.
 */
@Composable
internal fun CollectiblesLoadMoreEffect(listState: LazyListState, isEnabled: Boolean, onLoadMore: () -> Unit) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val shouldLoadMore by remember(listState, isEnabled) {
        derivedStateOf {
            if (!isEnabled) return@derivedStateOf false

            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            lastVisibleIndex != null && totalItems > 0 && lastVisibleIndex >= totalItems - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) currentOnLoadMore()
    }
}