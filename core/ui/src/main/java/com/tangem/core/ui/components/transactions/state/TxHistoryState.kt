package com.tangem.core.ui.components.transactions.state

import androidx.paging.PagingData
import androidx.paging.TerminalSeparatorType
import androidx.paging.insertHeaderItem
import com.tangem.core.ui.components.wallet.WalletLockedContentState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Wallet transaction history state */
sealed interface TxHistoryState {

    /**
     * Wallet transaction history state with content. Items contains a required [TxHistoryItemState.Title].
     *
     * @property contentItems content items
     */
    sealed class ContentState(private val contentItems: Flow<PagingData<TxHistoryItemState>>) : TxHistoryState {

        /** Lambda be invoke when explore button was clicked */
        abstract val onExploreClick: () -> Unit

        /** Content items with [TxHistoryItemState.Title] */
        val items: Flow<PagingData<TxHistoryItemState>>
            get() {
                return contentItems.map {
                    it.insertHeaderItem(
                        terminalSeparatorType = TerminalSeparatorType.SOURCE_COMPLETE,
                        item = TxHistoryItemState.Title(onExploreClick = onExploreClick),
                    )
                }
            }
    }

    /**
     * Loading state
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     * @property transactions   loading transactions
     */
    data class Loading(
        override val onExploreClick: () -> Unit,
        val transactions: Flow<PagingData<TxHistoryItemState>> = getDefaultLoadingTransactions(),
    ) : ContentState(transactions)

    /**
     * Wallet transaction history state with content
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     * @property contentItems content items
     */
    data class Content(
        override val onExploreClick: () -> Unit,
        val contentItems: Flow<PagingData<TxHistoryItemState>>,
    ) : ContentState(contentItems)

    /**
     * Locked state
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     */
    data class Locked(override val onExploreClick: () -> Unit) :
        ContentState(contentItems = getDefaultLoadingTransactions()),
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

        private fun getDefaultLoadingTransactions(): Flow<PagingData<TxHistoryItemState>> {
            return flowOf(
                value = PagingData.from(
                    data = listOf(
                        element = TxHistoryItemState.Transaction(
                            state = TransactionState.Loading(txHash = LOADING_TX_HASH),
                        ),
                    ),
                ),
            )
        }
    }
}