package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.transactions.Transaction
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState

/**
 * Single currency content item
 *
 * @param state    state
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@Composable
internal fun SingleCurrencyContentItem(state: WalletTxHistoryState.TxHistoryItemState, modifier: Modifier = Modifier) {
    when (state) {
        is WalletTxHistoryState.TxHistoryItemState.GroupTitle -> {
            TxHistoryGroupTitle(config = state, modifier = modifier)
        }
        is WalletTxHistoryState.TxHistoryItemState.Title -> {
            TxHistoryTitle(config = state, modifier = modifier)
        }
        is WalletTxHistoryState.TxHistoryItemState.Transaction -> {
            Transaction(state = state.state, modifier = modifier)
        }
    }
}
