package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.features.tangempay.components.txHistory.DefaultTangemPayTxHistoryComponent
import com.tangem.features.tangempay.entity.TangemPayTxHistoryUM
import com.tangem.features.tangempay.utils.TangemPayTxHistoryListManager
import com.tangem.features.tangempay.utils.TangemPayTxHistoryUpdateListener
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayTxHistoryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    tangemPayTxHistoryRepository: TangemPayTxHistoryRepository,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val txHistoryUpdateListener: TangemPayTxHistoryUpdateListener,
) : Model() {

    private val params: DefaultTangemPayTxHistoryComponent.Params = paramsContainer.require()
    private val listManager = TangemPayTxHistoryListManager(
        repository = tangemPayTxHistoryRepository,
        dispatchers = dispatchers,
        customerWalletAddress = params.customerWalletAddress,
        txHistoryUiActions = params.uiActions,
    )

    val uiState: StateFlow<TangemPayTxHistoryUM>
        field = MutableStateFlow<TangemPayTxHistoryUM>(getLoadingState(isBalanceHidden = true))

    init {
        handleBalanceHiding()
        launchPagination()
        subscribeToUiItemChanges()
        subscribeToUpdateListener()
    }

    private fun launchPagination() {
        modelScope.launch { listManager.launchPagination() }
    }

    private fun subscribeToUiItemChanges() {
        listManager.uiItems
            .onEach(::updateState)
            .launchIn(modelScope)
        listManager.paginationStatus
            .onEach(::handlePaginationStatus)
            .launchIn(modelScope)
        listManager.emptyStatus
            .onEach(::handleEmptyState)
            .launchIn(modelScope)
    }

    private fun subscribeToUpdateListener() {
        txHistoryUpdateListener.updates
            .onEach { reload() }
            .launchIn(modelScope)
    }

    private fun updateState(items: ImmutableList<TangemPayTxHistoryUM.TangemPayTxHistoryItemUM>) {
        if (items.isEmpty()) return // fast exit. If items is empty, no need to update ui items

        uiState.update { state ->
            if (state is TangemPayTxHistoryUM.Content) {
                state.copy(items = items)
            } else {
                TangemPayTxHistoryUM.Content(
                    items = items,
                    isBalanceHidden = state.isBalanceHidden,
                    loadMore = ::loadMoreItems,
                )
            }
        }
    }

    private fun handleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            uiState.update { getEmptyState(it.isBalanceHidden) }
        }
    }

    private fun handlePaginationStatus(status: PaginationStatus<*>) {
        uiState.update { state ->
            when (status) {
                is PaginationStatus.InitialLoadingError -> getErrorState(state.isBalanceHidden)
                PaginationStatus.EndOfPagination,
                PaginationStatus.InitialLoading,
                PaginationStatus.NextBatchLoading,
                PaginationStatus.None,
                is PaginationStatus.Paginating<*>,
                -> state
            }
        }
    }

    private fun loadMoreItems(): Boolean {
        modelScope.launch { listManager.loadMore(params.customerWalletAddress) }
        return true
    }

    fun reload() {
        uiState.update { state ->
            state as? TangemPayTxHistoryUM.Content ?: getLoadingState(state.isBalanceHidden)
        }
        modelScope.launch { listManager.reload() }
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .onEach { uiState.update { state -> state.copySealed(isBalanceHidden = it.isBalanceHidden) } }
            .launchIn(modelScope)
    }

    private fun getEmptyState(isBalanceHidden: Boolean): TangemPayTxHistoryUM.Empty {
        return TangemPayTxHistoryUM.Empty(isBalanceHidden = isBalanceHidden)
    }

    private fun getErrorState(isBalanceHidden: Boolean): TangemPayTxHistoryUM.Error {
        return TangemPayTxHistoryUM.Error(isBalanceHidden = isBalanceHidden, onReload = ::reload)
    }

    private fun getLoadingState(isBalanceHidden: Boolean): TangemPayTxHistoryUM.Loading {
        return TangemPayTxHistoryUM.Loading(isBalanceHidden = isBalanceHidden)
    }
}