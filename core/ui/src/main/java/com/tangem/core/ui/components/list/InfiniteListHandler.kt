package com.tangem.core.ui.components.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*

@Composable
fun InfiniteListHandler(
    listState: LazyListState,
    onLoadMore: () -> Boolean,
    buffer: Int = 2,
    triggerLoadMoreCheckOnItemsCountChange: Boolean = false,
) {
    val shouldLoadMore by remember(buffer, listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > totalItemsNumber - buffer
        }
    }

    val totalItemsCount by remember(listState) { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    var isEmitted by remember(key1 = totalItemsCount, key2 = buffer, key3 = listState) { mutableStateOf(false) }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isEmitted) {
            isEmitted = onLoadMore()
        }
    }

    LaunchedEffect(totalItemsCount) {
        if (triggerLoadMoreCheckOnItemsCountChange && shouldLoadMore && !isEmitted) {
            isEmitted = onLoadMore()
        }
    }
}