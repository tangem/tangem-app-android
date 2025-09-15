package com.tangem.features.tangempay.utils

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListBatchingContext
import com.tangem.domain.tangempay.model.TangemPayTxHistoryListConfig
import com.tangem.domain.tangempay.repository.TangemPayTxHistoryRepository
import com.tangem.domain.visa.model.VisaTxHistoryItem
import com.tangem.features.txhistory.entity.TxHistoryUM
import com.tangem.features.txhistory.utils.TxHistoryUiActions
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

private typealias TangemPayTxHistoryBatchAction = BatchAction<Int, TangemPayTxHistoryListConfig, Nothing>

internal class TangemPayTxHistoryListManager(
    private val repository: TangemPayTxHistoryRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userWalletId: UserWalletId,
    private val txHistoryUiActions: TxHistoryUiActions,
) {
    private val jobHolder = JobHolder()
    private val actionsFlow: MutableSharedFlow<TangemPayTxHistoryBatchAction> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val state: MutableStateFlow<TangemPayTxHistoryState> = MutableStateFlow(TangemPayTxHistoryState())
    private val uiManager = TangemPayTxHistoryUiManager(state = state, txHistoryUiActions = txHistoryUiActions)

    val uiItems: Flow<ImmutableList<TxHistoryUM.TxHistoryItemUM>> = uiManager.items
    val paginationStatus: Flow<PaginationStatus<*>> = state.map { it.status }.distinctUntilChanged()

    suspend fun launchPagination() = coroutineScope {
        val batchFlow = repository.getTxHistoryBatchFlow(
            context = TangemPayTxHistoryListBatchingContext(actionsFlow = actionsFlow, coroutineScope = this),
            batchSize = 50,
        )

        batchFlow.state
            .onEach { state -> updateState(state) }
            .flowOn(dispatchers.default)
            .launchIn(scope = this)
            .saveIn(jobHolder)

        // Initial load
        reload()
    }

    suspend fun reload() {
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = TangemPayTxHistoryListConfig(userWalletId = userWalletId, refresh = true),
            ),
        )
    }

    suspend fun loadMore(userWalletId: UserWalletId) {
        actionsFlow.emit(
            BatchAction.LoadMore(
                requestParams = TangemPayTxHistoryListConfig(userWalletId, refresh = false),
            ),
        )
    }

    private fun updateState(batchListState: BatchListState<Int, List<VisaTxHistoryItem>>) {
        state.update { state ->
            val clearUiBatches =
                state.status is PaginationStatus.InitialLoading && batchListState.status is PaginationStatus.Paginating
            state.copy(
                status = batchListState.status,
                uiBatches = uiManager.createOrUpdateUiBatches(
                    newCurrencyBatches = batchListState.data,
                    clearUiBatches = clearUiBatches,
                ),
            )
        }
    }
}