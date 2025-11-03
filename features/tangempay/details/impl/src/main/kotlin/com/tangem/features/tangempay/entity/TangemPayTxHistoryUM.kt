package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.transactions.state.TxHistoryState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal sealed interface TangemPayTxHistoryUM {

    val isBalanceHidden: Boolean

    data class Loading(override val isBalanceHidden: Boolean) : TangemPayTxHistoryUM {
        val items = persistentListOf(
            TangemPayTxHistoryItemUM.Title,
            TangemPayTxHistoryItemUM.Transaction(TangemPayTransactionState.Loading("LOADING_TX_HASH_1")),
            TangemPayTxHistoryItemUM.Transaction(TangemPayTransactionState.Loading("LOADING_TX_HASH_2")),
            TangemPayTxHistoryItemUM.Transaction(TangemPayTransactionState.Loading("LOADING_TX_HASH_3")),
        )
    }

    data class Empty(override val isBalanceHidden: Boolean) : TangemPayTxHistoryUM
    data class Error(override val isBalanceHidden: Boolean, val onReload: () -> Unit) : TangemPayTxHistoryUM

    data class Content(
        override val isBalanceHidden: Boolean,
        val items: ImmutableList<TangemPayTxHistoryItemUM>,
        val loadMore: () -> Boolean,
    ) : TangemPayTxHistoryUM

    fun copySealed(isBalanceHidden: Boolean): TangemPayTxHistoryUM {
        return when (this) {
            is Content -> copy(isBalanceHidden = isBalanceHidden)
            is Empty -> copy(isBalanceHidden = isBalanceHidden)
            is Error -> copy(isBalanceHidden = isBalanceHidden)
            is Loading -> copy(isBalanceHidden = isBalanceHidden)
        }
    }

    sealed interface TangemPayTxHistoryItemUM {
        data object Title : TangemPayTxHistoryItemUM
        data class GroupTitle(val title: String, val itemKey: String) : TangemPayTxHistoryItemUM {
            val legacyGroupTitle = TxHistoryState.TxHistoryItemState.GroupTitle(title = title, itemKey = itemKey)
        }
        data class Transaction(val transaction: TangemPayTransactionState) : TangemPayTxHistoryItemUM
    }
}