package com.tangem.core.ui.components.transactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.transactions.state.TxHistoryState

@Composable
internal fun TxHistoryContentItem(state: TxHistoryState.TxHistoryItemState, modifier: Modifier = Modifier) {
    when (state) {
        is TxHistoryState.TxHistoryItemState.GroupTitle -> {
            TxHistoryGroupTitle(config = state, modifier = modifier)
        }
        is TxHistoryState.TxHistoryItemState.Title -> {
            TxHistoryTitle(config = state, modifier = modifier)
        }
        is TxHistoryState.TxHistoryItemState.Transaction -> {
            Transaction(state = state.state, modifier = modifier)
        }
    }
}