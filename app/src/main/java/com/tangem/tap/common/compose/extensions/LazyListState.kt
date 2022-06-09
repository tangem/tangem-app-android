package com.tangem.tap.common.compose.extensions

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

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
    val context = LocalContext.current
    val view = LocalView.current
    LaunchedEffect(firstVisibleItemIndex) {
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }
}