package com.tangem.features.txhistory.utils

import com.tangem.core.ui.DesignFeatureToggles
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.txhistory.model.TxHistoryListBatchingContext
import com.tangem.domain.txhistory.model.TxHistoryListConfig
import com.tangem.domain.txhistory.models.PaginationWrapper
import com.tangem.domain.txhistory.repository.TxHistoryRepositoryV2
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionItemUMConverter
import com.tangem.features.txhistory.converter.TxHistoryItemToTransactionStateConverter
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.state.TxHistoryItemsSnapshot
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*

private typealias TxHistoryBatchAction = BatchAction<Int, TxHistoryListConfig, Nothing>

@Suppress("LongParameterList")
internal class TxHistoryListManager(
    private val repository: TxHistoryRepositoryV2,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userWalletId: UserWalletId,
    private val currency: CryptoCurrency,
    private val designFeatureToggles: DesignFeatureToggles,
    private val txHistoryUiActions: TxHistoryUiActions,
    private val lookupDataFlow: Flow<TxHistoryLookupContext>,
    legacyTxHistoryItemConverter: TxHistoryItemToTransactionStateConverter,
) {

    private val jobHolder = JobHolder()
    private val autoLoadMoreJobHolder = JobHolder()
    private val actionsFlow: MutableSharedFlow<TxHistoryBatchAction> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    private val state: MutableStateFlow<TxHistoryListState> = MutableStateFlow(TxHistoryListState())
    private val uiManager = TxHistoryUiManager(state = state)
    private val legacyUiManager = TxHistoryLegacyUiManager(
        state = state,
        txHistoryItemConverter = legacyTxHistoryItemConverter,
        txHistoryUiActions = txHistoryUiActions,
    )

    val uiItems: Flow<TxHistoryItemsSnapshot> = if (designFeatureToggles.isRedesignEnabled) {
        uiManager.items.map(TxHistoryItemsSnapshot::Items)
    } else {
        legacyUiManager.items.map(TxHistoryItemsSnapshot::LegacyItems)
    }
    val paginationStatus: Flow<PaginationStatus<*>> = state.map { it.status }.distinctUntilChanged()

    suspend fun init() = coroutineScope {
        val batchFlow = repository.getTxHistoryBatchFlow(
            context = TxHistoryListBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = this,
            ),
            batchSize = 50,
        )

        batchFlow.state
            .onEach { batchState -> autoLoadMoreUntilScrollable(batchState) }
            .flowOn(dispatchers.default)
            .launchIn(scope = this)
            .saveIn(autoLoadMoreJobHolder)

        if (designFeatureToggles.isRedesignEnabled) {
            var previousLookup: TxHistoryLookupContext? = null
            combine(batchFlow.state, lookupDataFlow) { batchState, lookup -> batchState to lookup }
                .onEach { (batchState, lookup) ->
                    val isLookupChanged = previousLookup != null && previousLookup != lookup
                    previousLookup = lookup
                    updateState(batchState, lookup, isLookupChanged)
                }
                .flowOn(dispatchers.default)
                .launchIn(scope = this)
                .saveIn(jobHolder)
        } else {
            batchFlow.state
                .onEach { batchState -> updateState(batchState, lookupContext = null, isLookupChanged = false) }
                .flowOn(dispatchers.default)
                .launchIn(scope = this)
                .saveIn(jobHolder)
        }
    }

    suspend fun startLoading() {
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = TxHistoryListConfig(userWalletId, currency, shouldRefresh = false),
            ),
        )
    }

    suspend fun reload() {
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = TxHistoryListConfig(userWalletId, currency, shouldRefresh = true),
            ),
        )
    }

    suspend fun loadMore(userWalletId: UserWalletId, currency: CryptoCurrency) {
        actionsFlow.emit(
            BatchAction.LoadMore(
                requestParams = TxHistoryListConfig(userWalletId, currency, shouldRefresh = false),
            ),
        )
    }

    private fun updateState(
        batchListState: BatchListState<Int, PaginationWrapper<TxInfo>>,
        lookupContext: TxHistoryLookupContext?,
        isLookupChanged: Boolean,
    ) {
        state.update { state ->
            val isInitialToPaginating = state.status is PaginationStatus.InitialLoading &&
                batchListState.status is PaginationStatus.Paginating
            val shouldClearUiBatches = isInitialToPaginating || isLookupChanged
            val isRedesignEnabled = designFeatureToggles.isRedesignEnabled
            state.copy(
                status = batchListState.status,
                uiBatches = if (isRedesignEnabled) {
                    val converter = TxHistoryItemToTransactionItemUMConverter(
                        currency = currency,
                        txHistoryUiActions = txHistoryUiActions,
                        lookupContext = lookupContext,
                    )
                    uiManager.createOrUpdateUiBatches(
                        newCurrencyBatches = batchListState.data,
                        shouldClearUiBatches = shouldClearUiBatches,
                        converter = converter,
                    )
                } else {
                    state.uiBatches
                },
                legacyUiBatches = if (isRedesignEnabled) {
                    state.legacyUiBatches
                } else {
                    legacyUiManager.createOrUpdateUiBatches(
                        newCurrencyBatches = batchListState.data,
                        shouldClearUiBatches = shouldClearUiBatches,
                    )
                },
            )
        }
    }

    private suspend fun autoLoadMoreUntilScrollable(batchState: BatchListState<Int, PaginationWrapper<TxInfo>>) {
        val status = batchState.status as? PaginationStatus.Paginating ?: return
        val lastResult = status.lastResult as? BatchFetchResult.Success ?: return
        val loadedItemsCount = batchState.data.sumOf { batch -> batch.data.items.size }
        val shouldLoadMore = loadedItemsCount < AUTO_LOAD_MORE_TARGET_COUNT || lastResult.empty
        if (shouldLoadMore) {
            loadMore(userWalletId, currency)
        }
    }

    private companion object {
        /** Number of loaded items considered enough to make the list scrollable. */
        const val AUTO_LOAD_MORE_TARGET_COUNT = 20
    }
}