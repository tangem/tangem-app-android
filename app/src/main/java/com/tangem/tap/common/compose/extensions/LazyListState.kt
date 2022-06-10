package com.tangem.tap.common.compose.extensions

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalView
import com.tangem.tap.common.extensions.hideKeyboard

@Composable
fun LazyListState.OnBottomReached(loadMoreThreshold: Int, loadMore: () -> Unit) {
    require(loadMoreThreshold >= 0)
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                ?: return@derivedStateOf false
            lastVisibleItem.index >= layoutInfo.totalItemsCount - 1 - loadMoreThreshold
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) loadMore()
    }
}

@Composable
fun LazyListState.HideKeyboardOnScroll() {
    if (isScrollInProgress) {
        LocalView.current.hideKeyboard()
    }
}