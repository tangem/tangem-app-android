package com.tangem.features.txhistory.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionState
import com.tangem.features.txhistory.entity.TxHistoryItemsUM
import com.tangem.features.txhistory.entity.TxHistoryUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Owns the transaction history UI state and routes updates to either [legacyUiState] or
 * [uiState] based on [DesignFeatureToggles.isRedesignEnabled]. Only the active pipeline gets
 * emitted to; the inactive flow stays at its initial Loading value.
 */
@ModelScoped
internal class TxHistoryStateController @Inject constructor(
    private val designFeatureToggles: DesignFeatureToggles,
) {

    private val _legacyUiState: MutableStateFlow<TxHistoryUM> =
        MutableStateFlow(TxHistoryUM.Loading(isBalanceHidden = true, onExploreClick = {}))
    val legacyUiState: StateFlow<TxHistoryUM> = _legacyUiState

    private val _uiState: MutableStateFlow<TxHistoryItemsUM> =
        MutableStateFlow(TxHistoryItemsUM.Loading(isBalanceHidden = true, onExploreClick = {}))
    val uiState: StateFlow<TxHistoryItemsUM> = _uiState

    val isNotSupported: Boolean
        get() = if (designFeatureToggles.isRedesignEnabled) {
            _uiState.value is TxHistoryItemsUM.NotSupported
        } else {
            _legacyUiState.value is TxHistoryUM.NotSupported
        }

    fun setLoading(isBalanceHidden: Boolean, onExploreClick: () -> Unit) {
        if (designFeatureToggles.isRedesignEnabled) {
            _uiState.value = TxHistoryItemsUM.Loading(
                isBalanceHidden = isBalanceHidden,
                onExploreClick = onExploreClick,
            )
        } else {
            _legacyUiState.value = TxHistoryUM.Loading(
                isBalanceHidden = isBalanceHidden,
                onExploreClick = onExploreClick,
            )
        }
    }

    fun setLoadingIfNotContent(onExploreClick: () -> Unit) {
        if (designFeatureToggles.isRedesignEnabled) {
            _uiState.update { state ->
                state as? TxHistoryItemsUM.Content ?: TxHistoryItemsUM.Loading(state.isBalanceHidden, onExploreClick)
            }
        } else {
            _legacyUiState.update { state ->
                state as? TxHistoryUM.Content ?: TxHistoryUM.Loading(state.isBalanceHidden, onExploreClick)
            }
        }
    }

    fun setError(onReloadClick: () -> Unit, onExploreClick: () -> Unit) {
        if (designFeatureToggles.isRedesignEnabled) {
            _uiState.value = TxHistoryItemsUM.Error(
                isBalanceHidden = _uiState.value.isBalanceHidden,
                onReloadClick = onReloadClick,
                onExploreClick = onExploreClick,
            )
        } else {
            _legacyUiState.value = TxHistoryUM.Error(
                isBalanceHidden = _legacyUiState.value.isBalanceHidden,
                onReloadClick = onReloadClick,
                onExploreClick = onExploreClick,
            )
        }
    }

    fun setEmpty(onExploreClick: () -> Unit) {
        if (designFeatureToggles.isRedesignEnabled) {
            _uiState.value = TxHistoryItemsUM.Empty(
                isBalanceHidden = _uiState.value.isBalanceHidden,
                onExploreClick = onExploreClick,
            )
        } else {
            _legacyUiState.value = TxHistoryUM.Empty(
                isBalanceHidden = _legacyUiState.value.isBalanceHidden,
                onExploreClick = onExploreClick,
            )
        }
    }

    fun setNotSupported(onExploreClick: () -> Unit) {
        if (designFeatureToggles.isRedesignEnabled) {
            _uiState.value = TxHistoryItemsUM.NotSupported(
                isBalanceHidden = _uiState.value.isBalanceHidden,
                pendingTransactions = persistentListOf(),
                onExploreClick = onExploreClick,
            )
        } else {
            _legacyUiState.value = TxHistoryUM.NotSupported(
                isBalanceHidden = _legacyUiState.value.isBalanceHidden,
                pendingTransactions = persistentListOf(),
                onExploreClick = onExploreClick,
            )
        }
    }

    fun setContent(snapshot: TxHistoryItemsSnapshot, loadMore: () -> Boolean) {
        when (snapshot) {
            is TxHistoryItemsSnapshot.Items -> _uiState.update { state ->
                if (state is TxHistoryItemsUM.Content) {
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
            is TxHistoryItemsSnapshot.LegacyItems -> _legacyUiState.update { state ->
                if (state is TxHistoryUM.Content) {
                    state.copy(items = snapshot.items)
                } else {
                    TxHistoryUM.Content(
                        items = snapshot.items,
                        isBalanceHidden = state.isBalanceHidden,
                        loadMore = loadMore,
                    )
                }
            }
        }
    }

    fun updateLoadingMore(isLoadingMore: Boolean) {
        if (!designFeatureToggles.isRedesignEnabled) return
        _uiState.update { state ->
            if (state is TxHistoryItemsUM.Content && state.isLoadingMore != isLoadingMore) {
                state.copy(isLoadingMore = isLoadingMore)
            } else {
                state
            }
        }
    }

    fun updateBalanceHidden(isBalanceHidden: Boolean) {
        if (designFeatureToggles.isRedesignEnabled) {
            _uiState.update { state -> state.copySealed(isBalanceHidden = isBalanceHidden) }
        } else {
            _legacyUiState.update { state -> state.copySealed(isBalanceHidden = isBalanceHidden) }
        }
    }

    fun updatePendingTransactions(
        pendingTxs: () -> ImmutableList<TransactionItemUM>,
        legacyPendingTxs: () -> ImmutableList<TransactionState>,
    ) {
        if (designFeatureToggles.isRedesignEnabled) {
            _uiState.update { state ->
                if (state is TxHistoryItemsUM.NotSupported) {
                    state.copy(pendingTransactions = pendingTxs())
                } else {
                    state
                }
            }
        } else {
            _legacyUiState.update { state ->
                if (state is TxHistoryUM.NotSupported) {
                    state.copy(pendingTransactions = legacyPendingTxs())
                } else {
                    state
                }
            }
        }
    }
}