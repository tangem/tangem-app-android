package com.tangem.core.ui.components.transactions.state

import androidx.paging.PagingData
import kotlinx.coroutines.flow.MutableStateFlow

/** Wallet transaction history state */
sealed interface TxHistoryState {

    /**
     * Wallet transaction history state with content
     *
     * @property contentItems   content items
     */
    data class Content(val contentItems: MutableStateFlow<PagingData<TxHistoryItemState>>) : TxHistoryState

    /**
     * Empty state
     *
     * @property onBuyClick lambda be invoke when buy button was clicked
     */
    data class Empty(val onBuyClick: () -> Unit) : TxHistoryState

    /**
     * Not supported tx history state
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     */
    data class NotSupported(val onExploreClick: () -> Unit) : TxHistoryState

    /**
     * Error state
     *
     * @property onReloadClick lambda be invoke when reload button was clicked
     */
    data class Error(val onReloadClick: () -> Unit) : TxHistoryState

    /** Transactions history item state */
    sealed interface TxHistoryItemState {

        /**
         * Title item
         *
         * @property onExploreClick lambda be invoke when explore button was clicked
         */
        data class Title(val onExploreClick: () -> Unit) : TxHistoryItemState

        /**
         * Group title item
         *
         * @property title title
         */
        data class GroupTitle(val title: String) : TxHistoryItemState

        /**
         * Transaction item
         *
         * @property state transaction state
         */
        data class Transaction(val state: TransactionState) : TxHistoryItemState
    }

    companion object {
        private const val LOADING_TX_HASH = "LOADING_TX_HASH"

        fun getDefaultLoadingTransactions(onExploreClick: () -> Unit): PagingData<TxHistoryItemState> {
            return PagingData.from(
                data = listOf(
                    TxHistoryItemState.Title(onExploreClick = onExploreClick),
                    TxHistoryItemState.Transaction(
                        state = TransactionState.Loading(txHash = LOADING_TX_HASH),
                    ),
                ),
            )
        }
    }
}