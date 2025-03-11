package com.tangem.core.ui.components.transactions

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.transactions.state.TxHistoryState

@Composable
internal fun TxHistoryListItem(
    state: TxHistoryState.TxHistoryItemState,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is TxHistoryState.TxHistoryItemState.GroupTitle -> {
            TxHistoryGroupTitle(config = state, modifier = modifier)
        }
        is TxHistoryState.TxHistoryItemState.Title -> {
            TxHistoryTitle(onExploreClick = state.onExploreClick, modifier = modifier)
        }
        is TxHistoryState.TxHistoryItemState.Transaction -> {
            Transaction(
                state = state.state,
                isBalanceHidden = isBalanceHidden,
                modifier = modifier,
            )
        }
    }
}