package com.tangem.feature.wallet.presentation.wallet.state.model.holder

import androidx.paging.PagingData
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import kotlinx.coroutines.flow.MutableStateFlow

internal interface TxHistoryStateHolder {

    val txHistoryState: TxHistoryState
}

internal class LockedTxHistoryStateHolder(onExploreClick: () -> Unit) : TxHistoryStateHolder {

    override val txHistoryState: TxHistoryState = TxHistoryState.Content(
        contentItems = MutableStateFlow(
            value = PagingData.from(
                data = listOf(
                    TxHistoryState.TxHistoryItemState.Title(onExploreClick = onExploreClick),
                    TxHistoryState.TxHistoryItemState.Transaction(
                        state = TransactionState.Locked(txHash = "LOCKED_TX_HASH"),
                    ),
                ),
            ),
        ),
    )
}