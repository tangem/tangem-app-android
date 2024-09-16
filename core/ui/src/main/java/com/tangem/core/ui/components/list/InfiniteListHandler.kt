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
    val loadMore by remember(buffer, listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > totalItemsNumber - buffer
        }
    }

    val totalItemsCount by remember(listState) { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    var emitted by remember(totalItemsCount, buffer, listState) { mutableStateOf(false) }

    LaunchedEffect(loadMore) {
        if (loadMore && !emitted) {
            emitted = onLoadMore()
        }
    }

    LaunchedEffect(totalItemsCount) {
        if (triggerLoadMoreCheckOnItemsCountChange && loadMore && !emitted) {
            emitted = onLoadMore()
        }
    }
}