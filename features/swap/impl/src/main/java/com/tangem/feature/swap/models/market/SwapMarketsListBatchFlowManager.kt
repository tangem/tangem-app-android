package com.tangem.feature.swap.models.market

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.models.market.converter.SwapMarketsTokenItemConverter
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class SwapMarketsListBatchFlowManager(
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val batchFlowType: GetMarketsTokenListFlowUseCase.BatchFlowType,
    private val currentAppCurrency: Provider<AppCurrency>,
    private val currentSearchText: Provider<String?>,
    private val modelScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    private val actionsFlow = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()
    private val updateStateJob = JobHolder()

    private val batchFlow = getMarketsTokenListFlowUseCase(
        batchingContext = TokenListBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
        batchFlowType = batchFlowType,
    )

    private val resultBatches = MutableStateFlow(ResultBatches())
    private val uiBatches = resultBatches.map { it.uiBatches }

    val uiItems: StateFlow<ImmutableList<MarketsListItemUM>>
        get() = uiBatches
            .map { batches ->
                batches.asSequence()
                    .map { it.data }
                    .flatten()
                    .toImmutableList()
            }
            .distinctUntilChanged()
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = persistentListOf(),
            )

    val onLastBatchLoadedSuccess = batchFlow.state
        .distinctUntilChanged { old, new -> old.status == new.status && old.data.size == new.data.size }
        .mapNotNull { batchListState ->
            when (val status = batchListState.status) {
                is PaginationStatus.Paginating -> {
                    if (status.lastResult is BatchFetchResult.Success) {
                        batchListState.data.lastOrNull()?.key
                    } else {
                        null
                    }
                }
                is PaginationStatus.EndOfPagination -> {
                    batchListState.data.lastOrNull()?.key
                }
                else -> null
            }
        }

    val onFirstBatchLoadedSuccess = batchFlow.state
        .distinctUntilChanged { old, new -> old.status == new.status && old.data.size == new.data.size }
        .mapNotNull { batchListState ->
            when (val status = batchListState.status) {
                is PaginationStatus.Paginating -> {
                    if (status.lastResult is BatchFetchResult.Success) {
                        batchListState.data.size == 1
                    } else {
                        null
                    }
                }
                is PaginationStatus.EndOfPagination -> {
                    batchListState.data.size == 1
                }
                else -> null
            }
        }
        .filter { it }

    val isInInitialLoadingErrorState = batchFlow.state
        .map { it.status is PaginationStatus.InitialLoadingError }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val isSearchNotFoundState = batchFlow.state
        .map { batchListState ->
            currentSearchText().isNullOrEmpty().not() &&
                batchListState.status is PaginationStatus.EndOfPagination &&
                batchListState.data.isEmpty()
        }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    init {
        batchFlow.state
            .map { it.data }
            .distinctUntilChanged { a, b ->
                a.size == b.size &&
                    a.map { it.key } == b.map { it.key } &&
                    a.map { it.data }.flatten() == b.map { it.data }.flatten()
            }
            .onEach {
                coroutineScope {
                    launch {
                        updateState(it)
                    }.saveIn(updateStateJob)
                }
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private suspend fun updateState(newList: List<Batch<Int, List<TokenMarket>>>, forceUpdate: Boolean = false) =
        withContext(dispatchers.default) {
            resultBatches.update { resultBatches ->
                val items = resultBatches.uiBatches
                val previousList = resultBatches.processedItems

                val converter = SwapMarketsTokenItemConverter(appCurrency = currentAppCurrency())

                if (newList.isEmpty()) {
                    return@update ResultBatches(processedItems = emptyList())
                }

                val isInitialLoading =
                    forceUpdate || previousList.isNullOrEmpty() || newList.first().key != previousList.first().key

                val outItems = if (isInitialLoading) {
                    newList.map { batch ->
                        Batch(
                            key = batch.key,
                            data = converter.convertList(batch.data),
                        )
                    }
                } else {
                    if (previousList.size != newList.size) {
                        val keysToAdd = newList.map { it.key }.subtract(previousList.map { it.key }.toSet())
                        val newBatches = newList.filter { keysToAdd.contains(it.key) }

                        items + newBatches.map { batch ->
                            Batch(
                                key = batch.key,
                                data = converter.convertList(batch.data),
                            )
                        }
                    } else {
                        items.mapIndexed { batchIndex, batch ->
                            val prevBatch = previousList[batchIndex]
                            val newBatch = newList[batchIndex]
                            if (prevBatch == newBatch) return@mapIndexed batch

                            Batch(
                                key = batch.key,
                                data = batch.data.mapIndexed { index, marketsListItemUM ->
                                    val prevItem = prevBatch.data.getOrNull(index)
                                    val newItem = newBatch.data.getOrNull(index)
                                    if (prevItem != null && newItem != null) {
                                        converter.update(prevItem, marketsListItemUM, newItem)
                                    } else {
                                        newItem?.let { converter.convert(it) } ?: marketsListItemUM
                                    }
                                },
                            )
                        }
                    }
                }

                currentCoroutineContext().ensureActive()

                ResultBatches(
                    uiBatches = outItems,
                    processedItems = newList,
                )
            }
        }

    fun reload(searchText: String? = null) {
        modelScope.launch {
            resultBatches.value = ResultBatches()
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = TokenMarketListConfig(
                        fiatPriceCurrency = currentAppCurrency().code,
                        searchText = if (currentSearchText() == null) {
                            null
                        } else {
                            searchText ?: currentSearchText()
                        },
                        priceChangeInterval = TokenMarketListConfig.Interval.H24,
                        order = TokenMarketListConfig.Order.ByRating,
                    ),
                ),
            )
        }
    }

    fun loadMore() {
        modelScope.launch {
            actionsFlow.emit(BatchAction.LoadMore())
        }
    }

    fun updateUIWithSameState() {
        modelScope.launch(dispatchers.default) {
            val current = batchFlow.state.value.data
            updateState(current, forceUpdate = true)
        }.saveIn(updateStateJob)
    }

    fun loadCharts(batchKeys: Set<Int>) {
        if (batchKeys.isEmpty()) return

        modelScope.launch {
            val currentData = batchFlow.state.value.data
            val alreadyLoadedChartsBatchKeys = currentData
                .filter { batch ->
                    val first = batch.data.firstOrNull() ?: return@filter false
                    first.tokenCharts.h24 != null
                }
                .map { it.key }
                .toSet()

            val batchesKeysToLoad = batchKeys.minus(alreadyLoadedChartsBatchKeys)

            if (batchesKeysToLoad.isNotEmpty()) {
                actionsFlow.emit(
                    BatchAction.UpdateBatches(
                        keys = batchesKeysToLoad,
                        updateRequest = TokenMarketUpdateRequest.UpdateChart(
                            interval = TokenMarketListConfig.Interval.H24,
                            currency = currentAppCurrency().code,
                        ),
                        async = true,
                        operationId = batchesKeysToLoad.toString() + "h24",
                    ),
                )
            }
        }
    }

    fun updateQuotes() {
        modelScope.launch {
            actionsFlow.emit(
                BatchAction.CancelUpdates {
                    it.updateRequest is TokenMarketUpdateRequest.UpdateQuotes
                },
            )

            actionsFlow.emit(
                BatchAction.UpdateBatches(
                    keys = batchFlow
                        .state
                        .value
                        .data
                        .map { it.key }
                        .toSet(),
                    updateRequest = TokenMarketUpdateRequest.UpdateQuotes(
                        currencyId = currentAppCurrency().code,
                    ),
                    async = true,
                    operationId = "update quotes",
                ),
            )
        }
    }

    fun clearStateAndStopAllActions() {
        resultBatches.value = ResultBatches()
        modelScope.launch {
            actionsFlow.emit(BatchAction.Reset)
        }
    }

    fun getBatchKeysByItemIds(ids: List<CryptoCurrency.RawID>): Set<Int> {
        val currentData = batchFlow.state.value.data

        return currentData
            .filter { d -> d.data.any { ids.contains(it.id) } }
            .map { it.key }
            .toSet()
    }

    fun getTokenById(id: CryptoCurrency.RawID): TokenMarket? {
        return batchFlow
            .state
            .value
            .data
            .map { it.data }
            .flatten()
            .find { it.id == id }
    }

    private data class ResultBatches(
        val uiBatches: List<Batch<Int, List<MarketsListItemUM>>> = emptyList(),
        val processedItems: List<Batch<Int, List<TokenMarket>>>? = null,
    )
}