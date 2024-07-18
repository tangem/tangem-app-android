package com.tangem.features.markets.model.statemanager

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.features.markets.model.converters.MarketsTokenItemConverter
import com.tangem.features.markets.model.utils.logAction
import com.tangem.features.markets.model.utils.logUpdateResults
import com.tangem.features.markets.ui.entity.MarketsListItemUM
import com.tangem.features.markets.ui.entity.MarketsListUM.TrendInterval
import com.tangem.features.markets.ui.entity.SortByTypeUM
import com.tangem.pagination.*
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

private const val LOG_EVENTS = false

internal class MarketsListUiItemsManager(
    private val logTag: String = "main",
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val currentTrendInterval: Provider<TrendInterval>,
    private val currentAppCurrency: Provider<AppCurrency>,
    private val modelScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    private val actionsFlow = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()

    private val batchFlow = getMarketsTokenListFlowUseCase(
        TokenListBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
    )

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
        .distinctUntilChanged { old, new -> old.status === new.status }
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

    private val uiBatches = MutableStateFlow<List<Batch<Int, List<MarketsListItemUM>>>>(emptyList())

    init {
        batchFlow.state
            .map { it.data }
            .distinctUntilChanged { a, b ->
                a.size == b.size && a.map { it.data }.flatten() == b.map { it.data }.flatten()
            }
            .onEachWithPrevious { prev, list ->
                updateState(prev, list)
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)

        if (LOG_EVENTS) {
            batchFlow.updateResults
                .onEach { logUpdateResults(logTag, it) }
                .launchIn(modelScope)

            actionsFlow
                .onEach { logAction(logTag, it) }
                .launchIn(modelScope)
        }
    }

    private fun updateState(
        previousList: List<Batch<Int, List<TokenMarket>>>?,
        list: List<Batch<Int, List<TokenMarket>>>,
        forceUpdate: Boolean = false,
    ) = uiBatches.update { items ->
        val converter = MarketsTokenItemConverter(currentTrendInterval(), appCurrency = currentAppCurrency())

        if (previousList == null || list.size < previousList.size || forceUpdate) {
            list.map {
                Batch(
                    key = it.key,
                    data = converter.convertList(it.data),
                )
            }
        } else {
            if (previousList.size != list.size) {
                val keysToAdd = list.map { it.key }.subtract(previousList.map { it.key }.toSet())
                val newBatches = list.filter { keysToAdd.contains(it.key) }

                items + newBatches.map {
                    Batch(
                        key = it.key,
                        data = converter.convertList(it.data),
                    )
                }
            } else {
                items.mapIndexed { batchIndex, batch ->
                    val prevBatch = previousList[batchIndex]
                    val newBatch = list[batchIndex]
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
    }

    fun reload(interval: TrendInterval, sortBy: SortByTypeUM) {
        modelScope.launch {
            uiBatches.value = emptyList()
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = TokenMarketListConfig(
                        fiatPriceCurrency = currentAppCurrency().code,
                        searchText = null,
                        showUnder100kMarketCapTokens = false,
                        priceChangeInterval = interval.toBatchRequestInterval(),
                        order = sortBy.toRequestOrder(),
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
            updateState(current, current, forceUpdate = true)
        }
    }

    fun loadCharts(batchKeys: Set<Int>, interval: TrendInterval) {
        modelScope.launch(dispatchers.default) {
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
                            interval = interval.toRequestInterval(),
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

    fun getBatchKeysByItemIds(ids: List<String>): Set<Int> {
        val currentData = batchFlow.state.value.data

        return currentData
            .filter { d -> d.data.any { ids.contains(it.id) } }
            .map { it.key }
            .toSet()
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

    private fun TrendInterval.toRequestInterval(): PriceChangeInterval {
        return when (this) {
            TrendInterval.H24 -> PriceChangeInterval.H24
            TrendInterval.D7 -> PriceChangeInterval.WEEK
            TrendInterval.M1 -> PriceChangeInterval.MONTH
        }
    }

    private fun <T> Flow<T>.onEachWithPrevious(operation: suspend (prev: T?, value: T) -> Unit): Flow<T> = flow {
        var prev: T? = null
        collect { value ->
            operation(prev, value)
            prev = value
            emit(value)
        }
    }
}
