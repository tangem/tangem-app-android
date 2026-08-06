package com.tangem.features.tangempay.txhistory

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.flow.StateFlow

internal interface TangemPayTxHistoryComponent {
    val state: StateFlow<TangemPayTxHistoryUM>
    fun LazyListScope.txHistoryContent(listState: LazyListState, state: TangemPayTxHistoryUM)
}