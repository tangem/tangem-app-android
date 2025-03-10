package com.tangem.features.txhistory.utils

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import com.tangem.domain.txhistory.model.TxHistoryListConfig
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.models.TxHistoryItem
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.entity.TxHistoryUM
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

private typealias TxHistoryBatchAction = BatchAction<Int, TxHistoryListConfig, Nothing>

internal class TxHistoryListManager(
    private val repository: TxHistoryRepositoryV2,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userWalletId: UserWalletId,
    private val currency: CryptoCurrency,
    txHistoryItemConverter: TxHistoryItemToTransactionStateConverter,
    txHistoryUiActions: TxHistoryUiActions,
) {

    private val jobHolder = JobHolder()
    private val actionsFlow: MutableSharedFlow<TxHistoryBatchAction> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val state: MutableStateFlow<TxHistoryListState> = MutableStateFlow(TxHistoryListState())
    private val uiManager = TxHistoryUiManager(
        state = state,
        txHistoryItemConverter = txHistoryItemConverter,
        txHistoryUiActions = txHistoryUiActions,
    )

    val uiItems: Flow<ImmutableList<TxHistoryUM.TxHistoryItemUM>> = uiManager.items

    suspend fun startLoading() = coroutineScope {
        val batchFlow = repository.getTxHistoryBatchFlow(
            context = TxHistoryListBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = this,
            ),
            batchSize = 50,
        )

        batchFlow.state
            .onEach { state -> updateState(state) }
            .flowOn(dispatchers.default)
            .launchIn(scope = this)
            .saveIn(jobHolder)

        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = TxHistoryListConfig(userWalletId, currency, refresh = false),
            ),
        )
    }

    suspend fun reload() {
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = TxHistoryListConfig(userWalletId, currency, refresh = true),
            ),
        )
    }

    suspend fun loadMore(userWalletId: UserWalletId, currency: CryptoCurrency) {
        actionsFlow.emit(
            BatchAction.LoadMore(
                requestParams = TxHistoryListConfig(userWalletId, currency, refresh = false),
            ),
        )
    }

    private fun updateState(batchListState: BatchListState<Int, PaginationWrapper<TxHistoryItem>>) {
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