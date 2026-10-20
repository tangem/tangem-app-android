package com.tangem.features.feed.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable

/**
 * Scroll position of a search tab's results together with the result set it belongs to. Owned by the
 * tab component, so both outlive the tab's composition.
 */
@Stable
class FeedSearchListState {

    val lazyListState: LazyListState = LazyListState()

    private var shownResults: Any? = null

    /**
     * Brings the list to the top when [results] — whatever identifies the result set, e.g. the query —
     * changes. The same results coming back into composition keep their position: on a tab switch, or
     * on return from a screen opened from them.
     */
    @Composable
    fun ScrollToTopOnNewResults(results: Any) {
        LaunchedEffect(results) {
            if (results != shownResults) {
                shownResults = results
                lazyListState.requestScrollToItem(0)
            }
        }
    }
}