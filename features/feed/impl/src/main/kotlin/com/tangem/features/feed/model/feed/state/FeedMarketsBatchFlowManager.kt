package com.tangem.features.feed.model.feed.state

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.model.converter.*
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class FeedMarketsBatchFlowManager(
    private val getTopFiveMarketTokenUseCase: GetTopFiveMarketTokenUseCase,
    private val currentAppCurrency: Provider<AppCurrency>,
    private val modelScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    private val managersByOrder = TokenMarketListConfig.Order.entries.associateWith { order ->
        createManagerForOrder(order)
    }

    val itemsByOrder: StateFlow<Map<SortByTypeUM, ImmutableList<MarketsListItemUM>>> =
        combine(
            TokenMarketListConfig.Order.entries.mapNotNull { order ->
                managersByOrder[order]?.uiItems?.map { items -> order to items }
            },
        ) { itemsList ->
            itemsList.associate { (order, items) ->
                val sortByType = order.toSortByTypeUM()
                sortByType to items
            }
        }.stateIn(
            scope = modelScope,
            started = Eagerly,
            initialValue = emptyMap(),
        )

    val loadingStatesByOrder: StateFlow<Map<SortByTypeUM, Boolean>> =
        combine(
            TokenMarketListConfig.Order.entries.mapNotNull { order ->
                managersByOrder[order]?.isLoading?.map { isLoading -> order to isLoading }
            },
        ) { loadingStatesList ->
            loadingStatesList.associate { (order, isLoading) ->
                val sortByType = order.toSortByTypeUM()
                sortByType to isLoading
            }
        }.stateIn(
            scope = modelScope,
            started = Eagerly,
            initialValue = emptyMap(),
        )

    val errorStatesByOrder: StateFlow<Map<SortByTypeUM, Boolean>> =
        combine(
            TokenMarketListConfig.Order.entries.mapNotNull { order ->
                managersByOrder[order]?.hasError?.map { hasError -> order to hasError }
            },
        ) { errorStatesList ->
            errorStatesList.associate { (order, hasError) ->
                val sortByType = order.toSortByTypeUM()
                sortByType to hasError
            }
        }.stateIn(
            scope = modelScope,
            started = Eagerly,
            initialValue = emptyMap(),
        )

    init {
        managersByOrder.values.forEach { manager ->
            manager.reload(currentAppCurrency().code)
        }
    }

    private fun createManagerForOrder(order: TokenMarketListConfig.Order): SingleOrderManager {
        val actionsFlow = MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>()

        val batchFlow = getTopFiveMarketTokenUseCase(
            batchingContext = TokenListBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = modelScope,
            ),
            order = order,
        )

        return SingleOrderManager(
            order = order,
            actionsFlow = actionsFlow,
            batchFlow = batchFlow,
            currentAppCurrency = currentAppCurrency,
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    fun reloadAll() {
        managersByOrder.values.forEach { manager ->
            manager.reload(currentAppCurrency().code)
        }
    }

    fun updateQuotes() {
        managersByOrder.values.forEach { manager ->
            manager.updateQuotes(currentAppCurrency().code)
        }
    }

    fun loadCharts(order: TokenMarketListConfig.Order) {
        managersByOrder[order]?.loadCharts()
    }

    fun getOnLastBatchLoadedSuccessFlow(order: TokenMarketListConfig.Order): Flow<Int>? {
        return managersByOrder[order]?.onLastBatchLoadedSuccess
    }

    fun getTokenMarketById(tokenId: CryptoCurrency.RawID): TokenMarket? {
        return managersByOrder.values
            .firstNotNullOfOrNull { manager -> manager.getTokenMarketById(tokenId) }
    }

    private class SingleOrderManager(
        val order: TokenMarketListConfig.Order,
        private val actionsFlow: MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>,
        private val batchFlow: TokenListBatchFlow,
        private val currentAppCurrency: Provider<AppCurrency>,
        private val modelScope: CoroutineScope,
        private val dispatchers: CoroutineDispatcherProvider,
    ) {
        private val updateStateJob = JobHolder()

        private val batchConverter = object : BatchItemConverter<TokenMarket, MarketsListItemUM> {

            private val internalConverter: MarketsTokenItemConverter
                get() = MarketsTokenItemConverter(
                    currentTrendInterval = MarketsListUM.TrendInterval.H24,
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

        val uiItems: StateFlow<ImmutableList<MarketsListItemUM>> =
            stateManager.state
                .map { it.uiBatches }
                .map { batches ->
                    batches.asSequence()
                        .map { it.data }
                        .flatten()
                        .toImmutableList()
                }
                .distinctUntilChanged()
                .stateIn(
                    scope = modelScope,
                    started = Eagerly,
                    initialValue = persistentListOf(),
                )

        val isLoading = batchFlow.state
            .map { state ->
                when (state.status) {
                    is PaginationStatus.InitialLoading -> true
                    is PaginationStatus.NextBatchLoading -> true
                    else -> false
                }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = modelScope,
                started = Eagerly,
                initialValue = false,
            )

        val hasError = batchFlow.state
            .map { state ->
                when (val status = state.status) {
                    is PaginationStatus.InitialLoadingError -> true
                    is PaginationStatus.Paginating -> status.lastResult is BatchFetchResult.Error
                    else -> false
                }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = modelScope,
                started = Eagerly,
                initialValue = false,
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
        }

        fun reload(fiatPriceCurrency: String) {
            modelScope.launch(dispatchers.default) {
                stateManager.state.value = BatchListState(
                    processedItems = emptyList(),
                    uiBatches = emptyList(),
                )
                actionsFlow.emit(
                    BatchAction.Reload(
                        requestParams = TokenMarketListConfig(
                            fiatPriceCurrency = fiatPriceCurrency,
                            searchText = null,
                            priceChangeInterval = TokenMarketListConfig.Interval.H24,
                            order = order,
                        ),
                    ),
                )
            }
        }

        fun updateQuotes(fiatPriceCurrency: String) {
            modelScope.launch(dispatchers.default) {
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
                            currencyId = fiatPriceCurrency,
                        ),
                        async = true,
                        operationId = "update quotes",
                    ),
                )
            }
        }

        fun loadCharts() {
            modelScope.launch(dispatchers.default) {
                val currentData = batchFlow.state.value.data
                val alreadyLoadedChartsBatchKeys = currentData
                    .filter { batch ->
                        val first = batch.data.firstOrNull() ?: return@filter false
                        first.tokenCharts.h24 != null
                    }
                    .map { it.key }
                    .toSet()

                val batchesKeysToLoad = currentData.map { it.key }.toSet().minus(alreadyLoadedChartsBatchKeys)

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

        fun getTokenMarketById(tokenId: CryptoCurrency.RawID): TokenMarket? {
            return stateManager.state.value.processedItems
                .asSequence()
                .flatMap { it.data }
                .firstOrNull { it.id == tokenId }
        }
    }

    private fun TokenMarketListConfig.Order.toSortByTypeUM(): SortByTypeUM {
        return when (this) {
            TokenMarketListConfig.Order.ByRating -> SortByTypeUM.Rating
            TokenMarketListConfig.Order.Trending -> SortByTypeUM.Trending
            TokenMarketListConfig.Order.Buyers -> SortByTypeUM.ExperiencedBuyers
            TokenMarketListConfig.Order.TopGainers -> SortByTypeUM.TopGainers
            TokenMarketListConfig.Order.TopLosers -> SortByTypeUM.TopLosers
            TokenMarketListConfig.Order.Staking -> SortByTypeUM.Staking
            TokenMarketListConfig.Order.YieldSupply -> SortByTypeUM.YieldSupply
        }
    }
}