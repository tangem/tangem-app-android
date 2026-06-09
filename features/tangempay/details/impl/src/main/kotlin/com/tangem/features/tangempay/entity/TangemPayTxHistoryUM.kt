package com.tangem.features.tangempay.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.transactions.state.TxHistoryState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal sealed interface TangemPayTxHistoryUM {

    val isBalanceHidden: Boolean

    data class Loading(override val isBalanceHidden: Boolean) : TangemPayTxHistoryUM {
        val items = persistentListOf(
            TangemPayTxHistoryItemUM.Title,
            TangemPayTxHistoryItemUM.GroupTitle(
                title = GROUP_TITLE_LOADING_PLACEHOLDER,
                itemKey = GROUP_TITLE_LOADING_ITEM_KEY,
                isLoading = true,
            ),
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

    @Immutable
    sealed interface TangemPayTxHistoryItemUM {
        data object Title : TangemPayTxHistoryItemUM
        data class GroupTitle(
            val title: String,
            val itemKey: String,
            val isLoading: Boolean = false,
        ) : TangemPayTxHistoryItemUM {
            val legacyGroupTitle = TxHistoryState.TxHistoryItemState.GroupTitle(title = title, itemKey = itemKey)
        }
        data class Transaction(val transaction: TangemPayTransactionState) : TangemPayTxHistoryItemUM
    }

    companion object {
        /** Placeholder copy for [TextShimmer] height; real date labels vary in width but not in line metrics. */
        const val GROUP_TITLE_LOADING_PLACEHOLDER: String = "Today"
        const val GROUP_TITLE_LOADING_ITEM_KEY: String = "loading-group-title"
    }
}