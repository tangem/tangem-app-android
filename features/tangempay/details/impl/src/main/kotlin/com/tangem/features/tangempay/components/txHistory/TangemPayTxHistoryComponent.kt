package com.tangem.features.tangempay.components.txHistory

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import com.tangem.features.txhistory.entity.TxHistoryUM
import kotlinx.coroutines.flow.StateFlow

internal interface TangemPayTxHistoryComponent {
    val state: StateFlow<TxHistoryUM>
    fun LazyListScope.txHistoryContent(listState: LazyListState, state: TxHistoryUM)
}