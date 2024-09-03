package com.tangem.core.ui.components.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*

@Composable
fun InfiniteListHandler(listState: LazyListState, onLoadMore: () -> Boolean, buffer: Int = 2) {
    val loadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1

            lastVisibleItemIndex > totalItemsNumber - buffer
        }
    }

    val totalItemsCount by remember { derivedStateOf { listState.layoutInfo.totalItemsCount } }
    var emitted by remember(totalItemsCount) { mutableStateOf(false) }

    LaunchedEffect(loadMore) {
        if (loadMore && !emitted) {
            emitted = onLoadMore()
        }
    }
}