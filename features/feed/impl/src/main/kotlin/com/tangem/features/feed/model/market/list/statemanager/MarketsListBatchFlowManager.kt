package com.tangem.features.feed.model.market.list.statemanager

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.model.converter.BatchItemConverter
import com.tangem.features.feed.model.converter.BatchListStateManager
import com.tangem.features.feed.model.converter.MarketsTokenItemConverter
import com.tangem.features.feed.model.converter.distinctBatchesContent
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.model.market.list.utils.logAction
import com.tangem.features.feed.model.market.list.utils.logStatus
import com.tangem.features.feed.model.market.list.utils.logUpdateResults
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

@Suppress("LongParameterList", "LargeClass")
internal class MarketsListBatchFlowManager(
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val batchFlowType: GetMarketsTokenListFlowUseCase.BatchFlowType,
    private val currentTrendInterval: Provider<MarketsListUM.TrendInterval>,
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

    private val batchConverter = object : BatchItemConverter<TokenMarket, MarketsListItemUM> {

        private val internalConverter: MarketsTokenItemConverter
            get() = MarketsTokenItemConverter(
                currentTrendInterval = currentTrendInterval(),
                appCurrency = currentAppCurrency(),
            )

        override fun convert(item: TokenMarket) = internalConverter.convert(item)

        override fun update(prevDomain: TokenMarket, currentUI: MarketsListItemUM, newDomain: TokenMarket) =
            internalConverter.update(prevDomain, currentUI, newDomain)
    }

    private val stateManager = BatchListStateManager<Int, TokenMarket, MarketsListItemUM>(
        converter = batchConverter,
        dispatchers = dispatchers,
    )

    private val resultBatches = MutableStateFlow(ResultBatches())
    private val uiBatches = stateManager.state.map { it.uiBatches }

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
            .distinctBatchesContent()
            .onEach { newList ->
                coroutineScope {
                    launch {
                        stateManager.update(newList = newList, forceUpdate = false)
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

    fun updateUIWithSameState() {
        modelScope.launch(dispatchers.default) {
            val current = batchFlow.state.value.data
            stateManager.update(current, forceUpdate = true)
        }.saveIn(updateStateJob)
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

    fun loadCharts(batchKeys: Set<Int>, interval: MarketsListUM.TrendInterval) {
        if (batchKeys.isEmpty()) return

        modelScope.launch {
            val currentData = batchFlow.state.value.data
            val alreadyLoadedChartsBatchKeys = currentData
                .filter { batch ->
                    val first = batch.data.firstOrNull() ?: return@filter false
                    val chartByInterval = when (interval) {
                        MarketsListUM.TrendInterval.H24 -> first.tokenCharts.h24
                        MarketsListUM.TrendInterval.D7 -> first.tokenCharts.week
                        MarketsListUM.TrendInterval.M1 -> first.tokenCharts.month
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

    private fun SortByTypeUM.toRequestOrder(): TokenMarketListConfig.Order {
        return when (this) {
            SortByTypeUM.Rating -> TokenMarketListConfig.Order.ByRating
            SortByTypeUM.Trending -> TokenMarketListConfig.Order.Trending
            SortByTypeUM.ExperiencedBuyers -> TokenMarketListConfig.Order.Buyers
            SortByTypeUM.TopGainers -> TokenMarketListConfig.Order.TopGainers
            SortByTypeUM.TopLosers -> TokenMarketListConfig.Order.TopLosers
            SortByTypeUM.Staking -> TokenMarketListConfig.Order.Staking
            SortByTypeUM.YieldSupply -> TokenMarketListConfig.Order.YieldSupply
        }
    }

    private fun MarketsListUM.TrendInterval.toBatchRequestInterval(): TokenMarketListConfig.Interval {
        return when (this) {
            MarketsListUM.TrendInterval.H24 -> TokenMarketListConfig.Interval.H24
            MarketsListUM.TrendInterval.D7 -> TokenMarketListConfig.Interval.WEEK
            MarketsListUM.TrendInterval.M1 -> TokenMarketListConfig.Interval.MONTH
        }
    }

    private data class ResultBatches(
        val uiBatches: List<Batch<Int, List<MarketsListItemUM>>> = emptyList(),
        val processedItems: List<Batch<Int, List<TokenMarket>>>? = null,
    )
}