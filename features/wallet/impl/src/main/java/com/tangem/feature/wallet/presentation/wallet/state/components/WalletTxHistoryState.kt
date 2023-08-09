package com.tangem.feature.wallet.presentation.wallet.state.components

import androidx.paging.PagingData
import com.tangem.core.ui.components.transactions.TransactionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Wallet transaction history state
 *
[REDACTED_AUTHOR]
 */
internal sealed interface WalletTxHistoryState {

    /**
     * Wallet transaction history state with content
     *
     * @property items content items
     */
    sealed class ContentState(open val items: Flow<PagingData<TxHistoryItemState>>) : WalletTxHistoryState

    /**
     * Loading state
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     */
    data class Loading(val onExploreClick: () -> Unit) : ContentState(
        items = flowOf(
            PagingData.from(
                listOf(
                    TxHistoryItemState.Title(onExploreClick = onExploreClick),
                    TxHistoryItemState.Transaction(state = TransactionState.Loading),
                ),
            ),
        ),
    )

    /**
     * Wallet transaction history state with loading transactions
     *
     * @property itemsCount count of loading transactions
     */
    data class ContentWithLoadingItems(val itemsCount: Int) : ContentState(
        items = flowOf(
            value = PagingData.from(
                data = buildList(capacity = itemsCount) {
                    add(TxHistoryItemState.Transaction(state = TransactionState.Loading))
                },
            ),
        ),
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
                        TxHistoryItemState.Transaction(state = TransactionState.Loading),
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
    data class Empty(val onBuyClick: () -> Unit) : WalletTxHistoryState

    /**
     * Not supported tx history state
     *
     * @property onExploreClick lambda be invoke when explore button was clicked
     */
    data class NotSupported(val onExploreClick: () -> Unit) : WalletTxHistoryState

    /**
     * Error state
     *
     * @property onReloadClick lambda be invoke when reload button was clicked
     */
    data class Error(val onReloadClick: () -> Unit) : WalletTxHistoryState

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
}