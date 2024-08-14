package com.tangem.features.markets.tokenlist.impl.model.statemanager

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.features.markets.tokenlist.impl.model.converters.MarketsTokenItemConverter
import com.tangem.features.markets.tokenlist.impl.model.utils.logAction
import com.tangem.features.markets.tokenlist.impl.model.utils.logStatus
import com.tangem.features.markets.tokenlist.impl.model.utils.logUpdateResults
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListItemUM
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListUM.TrendInterval
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByTypeUM
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

private const val LOG_EVENTS = true

@Suppress("LongParameterList")
internal class MarketsListBatchFlowManager(
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val batchFlowType: GetMarketsTokenListFlowUseCase.BatchFlowType,
    private val currentTrendInterval: Provider<TrendInterval>,
    private val currentAppCurrency: Provider<AppCurrency>,
    private val currentSearchText: Provider<String?>,
    private val currentSortByType: Provider<SortByTypeUM>,
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
        .mapNotNull {
            when (val status = it.status) {
                is PaginationStatus.Paginating -> {
                    if (status.lastResult is BatchFetchResult.Success) {
                        it.data.lastOrNull()?.key
                    } else {
                        null
                    }
                }
                is PaginationStatus.EndOfPagination -> {
                    it.data.lastOrNull()?.key
                }
                else -> null
            }
        }

    val isInInitialLoadingErrorState = batchFlow.state
        .map { it.status is PaginationStatus.InitialLoadingError }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val isSearchNotFoundState = batchFlow.state
        .map {
            currentSearchText().isNullOrEmpty().not() &&
                it.status is PaginationStatus.EndOfPagination &&
                it.data.isEmpty()
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

        if (LOG_EVENTS) {
            batchFlow.updateResults
                .onEach { logUpdateResults(batchFlowType.name, it) }
                .launchIn(modelScope)

            batchFlow.state
                .map { it.status }
                .onEach { logStatus(batchFlowType.name, it) }
                .launchIn(modelScope)

            actionsFlow
                .onEach { logAction(batchFlowType.name, it) }
                .launchIn(modelScope)
        }
    }

    private suspend fun updateState(newList: List<Batch<Int, List<TokenMarket>>>, forceUpdate: Boolean = false) =
        withContext(dispatchers.default) {
            resultBatches.update { resultBatches ->
                val items = resultBatches.uiBatches
                val previousList = resultBatches.processedItems

                val converter = MarketsTokenItemConverter(currentTrendInterval(), appCurrency = currentAppCurrency())

                if (newList.isEmpty()) {
                    return@update ResultBatches(processedItems = emptyList())
                }

                val isInitialLoading =
                    forceUpdate || previousList.isNullOrEmpty() || newList.first().key != previousList.first().key

                val outItems = if (isInitialLoading) {
                    newList.map {
                        Batch(
                            key = it.key,
                            data = converter.convertList(it.data),
                        )
                    }
                } else {
                    previousList!!
                    if (previousList.size != newList.size) {
                        val keysToAdd = newList.map { it.key }.subtract(previousList.map { it.key }.toSet())
                        val newBatches = newList.filter { keysToAdd.contains(it.key) }

                        items + newBatches.map {
                            Batch(
                                key = it.key,
                                data = converter.convertList(it.data),
                            )
                        }
                    } else {
                        items.mapIndexed { batchIndex, batch ->
                            val prevBatch = previousList[batchIndex]
                            val newBatch = newList[batchIndex]
                            if (previousList == newBatch) return@mapIndexed batch

                            Batch(
                                key = batch.key,
                                data = batch.data.mapIndexed { index, marketsListItemUM ->
                                    val prevItem = prevBatch.data[index]
                                    val newItem = newBatch.data[index]

                                    converter.update(
                                        prevItem,
                                        marketsListItemUM,
                                        newItem,
                                    )
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
                        priceChangeInterval = currentTrendInterval().toBatchRequestInterval(),
                        order = currentSortByType().toRequestOrder(),
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

    fun loadCharts(batchKeys: Set<Int>, interval: TrendInterval) {
        if (batchKeys.isEmpty()) return

        modelScope.launch {
            val currentData = batchFlow.state.value.data
            val alreadyLoadedChartsBatchKeys = currentData
                .filter {
                    val first = it.data.firstOrNull() ?: return@filter false
                    val chartByInterval = when (interval) {
                        TrendInterval.H24 -> first.tokenCharts.h24
                        TrendInterval.D7 -> first.tokenCharts.week
                        TrendInterval.M1 -> first.tokenCharts.month
                    }
                    chartByInterval != null
                }
                .map { it.key }
                .toSet()

            val batchesKeysToLoad = batchKeys.minus(alreadyLoadedChartsBatchKeys)

            if (batchesKeysToLoad.isNotEmpty()) {
                actionsFlow.emit(
                    BatchAction.UpdateBatches(
                        keys = batchesKeysToLoad,
                        updateRequest = TokenMarketUpdateRequest.UpdateChart(
                            interval = interval.toBatchRequestInterval(),
                            currency = currentAppCurrency().code,
                        ),
                        async = true,
                        operationId = batchesKeysToLoad.toString() + interval.toString(),
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
                    keys = batchFlow.state.value.data.map { it.key }.toSet(),
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

    fun getBatchKeysByItemIds(ids: List<String>): Set<Int> {
        val currentData = batchFlow.state.value.data

        return currentData
            .filter { d -> d.data.any { ids.contains(it.id) } }
            .map { it.key }
            .toSet()
    }

    fun getTokenById(id: String): TokenMarket? {
        return batchFlow.state.value.data.map { it.data }.flatten().find { it.id == id }
    }

    private fun SortByTypeUM.toRequestOrder(): TokenMarketListConfig.Order {
        return when (this) {
            SortByTypeUM.Rating -> TokenMarketListConfig.Order.ByRating
            SortByTypeUM.Trending -> TokenMarketListConfig.Order.Trending
            SortByTypeUM.ExperiencedBuyers -> TokenMarketListConfig.Order.Buyers
            SortByTypeUM.TopGainers -> TokenMarketListConfig.Order.TopGainers
            SortByTypeUM.TopLosers -> TokenMarketListConfig.Order.TopLosers
        }
    }

    private fun TrendInterval.toBatchRequestInterval(): TokenMarketListConfig.Interval {
        return when (this) {
            TrendInterval.H24 -> TokenMarketListConfig.Interval.H24
            TrendInterval.D7 -> TokenMarketListConfig.Interval.WEEK
            TrendInterval.M1 -> TokenMarketListConfig.Interval.MONTH
        }
    }

    private data class ResultBatches(
        val uiBatches: List<Batch<Int, List<MarketsListItemUM>>> = emptyList(),
        val processedItems: List<Batch<Int, List<TokenMarket>>>? = null,
    )
}