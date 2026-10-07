package com.tangem.features.txhistory.model

import com.tangem.domain.txhistory.list.HistoryTxListManager.HistoryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Whether the history holds pages past its first one — a reload would throw them away */
internal class LoadedPagesTracker {

    val hasLoadedMorePages: StateFlow<Boolean>
        field = MutableStateFlow(false)

    private var rowsBeforeLoadMore: Int? = null

    /** @param current the list the request is made on */
    fun onLoadMore(current: HistoryState) {
        // The list asks for more whenever its end shows, short histories included; only a real next page counts
        val content = current as? HistoryState.Content ?: return
        if (!content.hasMore) return

        hasLoadedMorePages.value = true
        rowsBeforeLoadMore = content.items.size
    }

    /** A request that ends without new rows (a failed page) loaded nothing */
    fun onState(state: HistoryState) {
        val content = state as? HistoryState.Content ?: return
        val rowsBefore = rowsBeforeLoadMore ?: return
        if (content.isLoadingMore) return

        rowsBeforeLoadMore = null
        if (content.items.size <= rowsBefore) hasLoadedMorePages.value = false
    }

    fun onReload() {
        rowsBeforeLoadMore = null
        hasLoadedMorePages.value = false
    }
}