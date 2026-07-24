package com.tangem.features.txhistory.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Owns the transaction history UI state and routes updates to [uiState].
 */
@ModelScoped
internal class TxHistoryStateController @Inject constructor() {

    /**
     * Pre-redesign state. Kept only to satisfy consumers that still read the legacy UI (they are
     * dead at runtime while the redesign is enabled); no longer populated.
     */
    val legacyUiState: StateFlow<TxHistoryUM> =
        MutableStateFlow(TxHistoryUM.Loading(isBalanceHidden = true, onExploreClick = {})).asStateFlow()

    private val _uiState: MutableStateFlow<TxHistoryItemsUM> =
        MutableStateFlow(TxHistoryItemsUM.Loading(isBalanceHidden = true, onExploreClick = {}))
    val uiState: StateFlow<TxHistoryItemsUM> = _uiState.asStateFlow()

    val isNotSupported: Boolean
        get() = _uiState.value is TxHistoryItemsUM.NotSupported

    fun setLoading(isBalanceHidden: Boolean, onExploreClick: () -> Unit) {
        _uiState.value = TxHistoryItemsUM.Loading(
            isBalanceHidden = isBalanceHidden,
            onExploreClick = onExploreClick,
        )
    }

    fun setLoadingIfNotContent(onExploreClick: () -> Unit) {
        _uiState.update { state ->
            state as? TxHistoryItemsUM.Content ?: TxHistoryItemsUM.Loading(state.isBalanceHidden, onExploreClick)
        }
    }

    fun setError(onReloadClick: () -> Unit, onExploreClick: () -> Unit) {
        _uiState.value = TxHistoryItemsUM.Error(
            isBalanceHidden = _uiState.value.isBalanceHidden,
            onReloadClick = onReloadClick,
            onExploreClick = onExploreClick,
        )
    }

    fun setEmpty(onExploreClick: () -> Unit) {
        _uiState.value = TxHistoryItemsUM.Empty(
            isBalanceHidden = _uiState.value.isBalanceHidden,
            onExploreClick = onExploreClick,
        )
    }

    fun setNotSupported(onExploreClick: () -> Unit) {
        _uiState.value = TxHistoryItemsUM.NotSupported(
            isBalanceHidden = _uiState.value.isBalanceHidden,
            pendingTransactions = persistentListOf(),
            onExploreClick = onExploreClick,
        )
    }

    fun setContent(snapshot: TxHistoryItemsSnapshot, loadMore: () -> Boolean, onExploreClick: () -> Unit) {
        when (snapshot) {
            is TxHistoryItemsSnapshot.Items -> _uiState.update { state ->
                if (snapshot.items.none { it is TxHistoryItemsUM.TxHistoryItemUM.Transaction }) {
                    TxHistoryItemsUM.Empty(
                        isBalanceHidden = state.isBalanceHidden,
                        onExploreClick = onExploreClick,
                    )
                } else if (state is TxHistoryItemsUM.Content) {
                    state.copy(items = snapshot.items)
                } else {
                    TxHistoryItemsUM.Content(
                        items = snapshot.items,
                        isBalanceHidden = state.isBalanceHidden,
                        isLoadingMore = false,
                        loadMore = loadMore,
                    )
                }
            }
        }
    }

    fun updateLoadingMore(isLoadingMore: Boolean) {
        _uiState.update { state ->
            if (state is TxHistoryItemsUM.Content && state.isLoadingMore != isLoadingMore) {
                state.copy(isLoadingMore = isLoadingMore)
            } else {
                state
            }
        }
    }

    fun updateBalanceHidden(isBalanceHidden: Boolean) {
        _uiState.update { state -> state.copySealed(isBalanceHidden = isBalanceHidden) }
    }

    fun updatePendingTransactions(pendingTxs: () -> ImmutableList<TransactionItemUM>) {
        _uiState.update { state ->
            if (state is TxHistoryItemsUM.NotSupported) {
                state.copy(pendingTransactions = pendingTxs())
            } else {
                state
            }
        }
    }
}