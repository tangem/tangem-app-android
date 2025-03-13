package com.tangem.features.txhistory.entity

import com.tangem.core.ui.components.transactions.state.TransactionState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

sealed interface TxHistoryUM {

    val isBalanceHidden: Boolean

    data class Loading(override val isBalanceHidden: Boolean, private val onExploreClick: () -> Unit) : TxHistoryUM {
        val items = persistentListOf(
            TxHistoryItemUM.Title(onExploreClick = onExploreClick),
            TxHistoryItemUM.Transaction(TransactionState.Loading("LOADING_TX_HASH_1")),
            TxHistoryItemUM.Transaction(TransactionState.Loading("LOADING_TX_HASH_2")),
            TxHistoryItemUM.Transaction(TransactionState.Loading("LOADING_TX_HASH_3")),
        )
    }

    /**
     * Wallet transaction history state with content
     */
    data class Content(
        override val isBalanceHidden: Boolean,
        val items: ImmutableList<TxHistoryItemUM>,
        val loadMore: () -> Boolean,
    ) : TxHistoryUM

    /** Empty state */
    data class Empty(override val isBalanceHidden: Boolean, val onExploreClick: () -> Unit) : TxHistoryUM

    /**
     * Not supported tx history state
     *
     * @property pendingTransactions pending transactions
     * @property onExploreClick      lambda be invoke when explore button was clicked
     */
    data class NotSupported(
        override val isBalanceHidden: Boolean,
        val pendingTransactions: ImmutableList<TransactionState>,
        val onExploreClick: () -> Unit,
    ) : TxHistoryUM

    /**
     * Error state
     *
     * @property onReloadClick lambda be invoke when reload button was clicked
     */
    data class Error(
        override val isBalanceHidden: Boolean,
        val onReloadClick: () -> Unit,
        val onExploreClick: () -> Unit,
    ) : TxHistoryUM

    fun copySealed(isBalanceHidden: Boolean): TxHistoryUM {
        return when (this) {
            is Content -> copy(isBalanceHidden = isBalanceHidden)
            is NotSupported -> copy(isBalanceHidden = isBalanceHidden)
            is Empty -> copy(isBalanceHidden = isBalanceHidden)
            is Error -> copy(isBalanceHidden = isBalanceHidden)
            is Loading -> copy(isBalanceHidden = isBalanceHidden)
        }
    }

    /** Transactions history item state */
    sealed interface TxHistoryItemUM {

        /**
         * Title item
         *
         * @property onExploreClick lambda be invoke when explore button was clicked
         */
        data class Title(val onExploreClick: () -> Unit) : TxHistoryItemUM

        /**
         * Group title item
         *
         * @property title title
         * @property itemKey key to use in compose
         */
        data class GroupTitle(
            val title: String,
            val itemKey: String,
        ) : TxHistoryItemUM

        /**
         * Transaction item
         *
         * @property state transaction state
         */
        data class Transaction(val state: TransactionState) : TxHistoryItemUM
    }
}