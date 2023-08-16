package com.tangem.core.ui.components.transactions.state

import androidx.paging.PagingData
import androidx.paging.insertHeaderItem
import com.tangem.core.ui.components.wallet.WalletLockedContentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Wallet transaction history state */
sealed interface TxHistoryState {

    /**
     * Wallet transaction history state with content
     *
     * @property items content items
     */
    sealed class ContentState(open val items: Flow<PagingData<TxHistoryItemState>>) : TxHistoryState

    /**
     * Loading state. Items contains a required [TxHistoryItemState.Title].
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     * @property items          loading transactions
     */
    data class Loading(
        val onExploreClick: () -> Unit,
        override val items: Flow<PagingData<TxHistoryItemState>> = flowOf(
            PagingData.from(
                listOf(
                    element = TxHistoryItemState.Transaction(
                        state = TransactionState.Loading(txHash = LOADING_TX_HASH),
                    ),
                ),
            ),
        ),
    ) : ContentState(
        items = items.map {
            it.insertHeaderItem(item = TxHistoryItemState.Title(onExploreClick = onExploreClick))
        },
    )

    /**
     * Wallet transaction history state with content
     *
     * @property items content items
     */
    data class Content(override val items: Flow<PagingData<TxHistoryItemState>>) : ContentState(items)

    /**
     * Locked state
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     */
    data class Locked(val onExploreClick: () -> Unit) :
        ContentState(
            items = flowOf(
                PagingData.from(
                    listOf(
                        TxHistoryItemState.Title(onExploreClick = onExploreClick),
                        TxHistoryItemState.Transaction(state = TransactionState.Loading(txHash = LOADING_TX_HASH)),
                    ),
                ),
            ),
        ),
        WalletLockedContentState

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

    private companion object {
        const val LOADING_TX_HASH = "LOADING_TX_HASH"
    }
}
