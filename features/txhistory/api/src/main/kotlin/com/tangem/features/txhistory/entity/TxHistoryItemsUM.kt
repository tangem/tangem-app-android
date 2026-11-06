package com.tangem.features.txhistory.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import kotlinx.collections.immutable.ImmutableList

/**
 * Transaction history state for Token Details. Uses [TransactionItemUM] for transaction items so the
 * `TransactionItem` composable can render structured fields without parsing.
 */
@Immutable
sealed interface TxHistoryItemsUM {

    val isBalanceHidden: Boolean

    data class Loading(
        override val isBalanceHidden: Boolean,
        val onExploreClick: () -> Unit,
    ) : TxHistoryItemsUM {

        companion object {
            /** How many shimmer rows the loading block draws, fading out towards the last one. */
            const val SHIMMER_ROW_COUNT = 4
        }
    }

    data class Content(
        override val isBalanceHidden: Boolean,
        val items: ImmutableList<TxHistoryItemUM>,
        val isLoadingMore: Boolean,
        val loadMore: () -> Boolean,
    ) : TxHistoryItemsUM

    data class Empty(override val isBalanceHidden: Boolean, val onExploreClick: () -> Unit) : TxHistoryItemsUM

    data class NotSupported(
        override val isBalanceHidden: Boolean,
        val pendingTransactions: ImmutableList<TransactionItemUM>,
        val onExploreClick: () -> Unit,
    ) : TxHistoryItemsUM

    data class Error(
        override val isBalanceHidden: Boolean,
        val onReloadClick: () -> Unit,
        val onExploreClick: () -> Unit,
    ) : TxHistoryItemsUM

    fun copySealed(isBalanceHidden: Boolean): TxHistoryItemsUM {
        return when (this) {
            is Content -> copy(isBalanceHidden = isBalanceHidden)
            is NotSupported -> copy(isBalanceHidden = isBalanceHidden)
            is Empty -> copy(isBalanceHidden = isBalanceHidden)
            is Error -> copy(isBalanceHidden = isBalanceHidden)
            is Loading -> copy(isBalanceHidden = isBalanceHidden)
        }
    }

    @Immutable
    sealed interface TxHistoryItemUM {

        data class GroupTitle(
            val title: String,
            val itemKey: String,
        ) : TxHistoryItemUM

        data class Transaction(val state: TransactionItemUM) : TxHistoryItemUM
    }
}