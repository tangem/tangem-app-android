package com.tangem.tap.common.compose.extensions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*

@Composable
fun LazyListState.OnBottomReached(loadMoreThreshold: Int, loadMore: () -> Unit) {
    require(loadMoreThreshold >= 0)
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >=  layoutInfo.totalItemsCount - 1 - loadMoreThreshold
        }
    }

    LaunchedEffect(shouldLoadMore) {
        snapshotFlow { shouldLoadMore.value }
            .collect { shouldLoadMore ->
                if (shouldLoadMore) loadMore()
            }
    }
}