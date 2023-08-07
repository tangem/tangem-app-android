package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateHolder
import com.tangem.feature.wallet.presentation.wallet.state.content.WalletTxHistoryState
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
        is WalletStateHolder.MultiCurrencyContent -> tokensListItems(state.tokensListState, modifier)
        is WalletStateHolder.SingleCurrencyContent -> txHistoryItems(state.txHistoryState, txHistoryItems, modifier)
        is WalletStateHolder.Loading,
        is WalletStateHolder.UnlockWalletContent,
        -> Unit
    }
}