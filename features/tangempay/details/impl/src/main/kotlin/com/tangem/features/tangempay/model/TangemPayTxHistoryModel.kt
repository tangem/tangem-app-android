package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.features.tangempay.components.txHistory.DefaultTangemPayTxHistoryComponent
import com.tangem.features.tangempay.utils.TangemPayTxHistoryListManager
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayTxHistoryModel @Inject constructor(
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    tangemPayTxHistoryRepository: TangemPayTxHistoryRepository,
    paramsContainer: ParamsContainer,
) : Model(), TxHistoryUiActions {

    private val params: DefaultTangemPayTxHistoryComponent.Params = paramsContainer.require()
    private val listManager = TangemPayTxHistoryListManager(
        repository = tangemPayTxHistoryRepository,
        dispatchers = dispatchers,
        customerWalletAddress = params.customerWalletAddress,
        txHistoryUiActions = this,
    )

    val uiState: StateFlow<TxHistoryUM>
        field = MutableStateFlow<TxHistoryUM>(getLoadingState(isBalanceHidden = true))

    init {
        handleBalanceHiding()
        launchPagination()
        subscribeToUiItemChanges()
    }

    private fun launchPagination() {
        modelScope.launch { listManager.launchPagination() }
    }

    private fun subscribeToUiItemChanges() {
        listManager.uiItems
            .onEach { updateState(it) }
            .launchIn(modelScope)
        listManager.paginationStatus
            .onEach { paginationStatus -> handlePaginationStatus(paginationStatus) }
            .launchIn(modelScope)
    }

    private fun updateState(items: ImmutableList<TxHistoryUM.TxHistoryItemUM>) {
        uiState.update { state ->
            if (state is TxHistoryUM.Content) {
                state.copy(items = items)
            } else {
                TxHistoryUM.Content(
                    items = items,
                    isBalanceHidden = state.isBalanceHidden,
                    loadMore = ::loadMoreItems,
                )
            }
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
        // fast exit
        if (uiState.value is TxHistoryUM.NotSupported) return

        uiState.update { state ->
            state as? TxHistoryUM.Content ?: getLoadingState(state.isBalanceHidden)
        }
        modelScope.launch { listManager.reload() }
    }

    private fun handleBalanceHiding() {
        getBalanceHidingSettingsUseCase()
            .onEach { uiState.update { state -> state.copySealed(isBalanceHidden = it.isBalanceHidden) } }
            .launchIn(modelScope)
    }

    override fun openExplorer() {
        Timber.d("onExploreClick: open explorer")
    }

    override fun openTxInExplorer(txHash: String) {
        Timber.d("openTxInExplorer: $txHash")
    }

    private fun getErrorState(isBalanceHidden: Boolean): TxHistoryUM.Error {
        return TxHistoryUM.Error(
            isBalanceHidden = isBalanceHidden,
            onReloadClick = ::reload,
            onExploreClick = ::openExplorer,
        )
    }

    private fun getLoadingState(isBalanceHidden: Boolean): TxHistoryUM.Loading {
        return TxHistoryUM.Loading(isBalanceHidden = isBalanceHidden, onExploreClick = ::openExplorer)
    }
}