package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import com.tangem.core.ui.components.transactions.txHistoryItems
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletState
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.tokensListItems
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.tokensListItemsV2
import com.tangem.feature.wallet.presentation.wallet.state2.model.WalletState as WalletStateV2

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
    state: WalletState.ContentState,
    txHistoryItems: LazyPagingItems<TxHistoryState.TxHistoryItemState>?,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletMultiCurrencyState -> tokensListItems(state.tokensListState, modifier, isBalanceHidden)
        is WalletSingleCurrencyState -> txHistoryItems(state.txHistoryState, txHistoryItems, isBalanceHidden, modifier)
    }
}

internal fun LazyListScope.contentItemsV2(
    state: WalletStateV2,
    txHistoryItems: LazyPagingItems<TxHistoryState.TxHistoryItemState>?,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletStateV2.MultiCurrency -> {
            tokensListItemsV2(state.tokensListState, modifier, isBalanceHidden)
        }
        is WalletStateV2.SingleCurrency -> {
            txHistoryItems(state.txHistoryState, txHistoryItems, isBalanceHidden, modifier)
        }
        is WalletStateV2.Visa.Content -> {
            // TODO: Will be implemented soon
        }
        is WalletStateV2.Visa.Locked -> {
            // TODO: Will be implemented soon
        }
    }
}