package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.tangem.feature.wallet.presentation.wallet.state.WalletLoading
import com.tangem.feature.wallet.presentation.wallet.state.WalletMultiCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletSingleCurrencyState
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletTxHistoryState
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.tokensListItems
import com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency.txHistoryItems

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
    state: WalletStateHolder,
    txHistoryItems: LazyPagingItems<WalletTxHistoryState.TxHistoryItemState>?,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is WalletMultiCurrencyState -> tokensListItems(state.tokensListState, modifier)
        is WalletSingleCurrencyState -> txHistoryItems(state.txHistoryState, txHistoryItems, modifier)
        is WalletLoading,
        -> Unit
    }
}