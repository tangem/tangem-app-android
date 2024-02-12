package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.txHistoryItems
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.tokensListItems

/**
 * Wallet content
 *
 * @param state          wallet state
 * @param txHistoryItems transaction history items
 * @param modifier       modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.contentItems(
    state: WalletState,
    txHistoryItems: LazyPagingItems<TxHistoryState.TxHistoryItemState>?,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletState.MultiCurrency -> {
            tokensListItems(state.tokensListState, modifier, isBalanceHidden)
        }
        is WalletState.SingleCurrency -> {
            txHistoryItems(state.txHistoryState, txHistoryItems, isBalanceHidden, modifier)
        }
        is WalletState.Visa -> {
            txHistoryItems(state.txHistoryState, txHistoryItems, isBalanceHidden, modifier)
        }
    }
}