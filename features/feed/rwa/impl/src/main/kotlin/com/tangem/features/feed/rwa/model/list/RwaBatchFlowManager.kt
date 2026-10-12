package com.tangem.features.feed.rwa.model.list

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.rwa.ui.state.RwaListItemUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Paginated list for the Real world assets block: the crypto tab's Market Pulse manager plus the
 * category/sector filter. The config is read from [request] on every [reload], so a changed filter
 * only needs a `reload()`.
 *
 * The search tab uses a second instance with [batchFlowType] `Search` and a [request] carrying a
 * `searchText`; the two modes never share an instance, because their request configs differ and a single
 * batch flow would thrash between them.
 */
internal class RwaBatchFlowManager(
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val request: StateFlow<RwaListRequest>,
    private val appCurrency: StateFlow<AppCurrency>,
    private val onItemClick: (CryptoCurrency.RawID) -> Unit,
    private val modelScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    batchFlowType: GetMarketsTokenListFlowUseCase.BatchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
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

    private val batchConverter = object : BatchItemConverter<TokenMarket, RwaListItemUM> {

        private val internalConverter: RwaListItemConverter
            get() = RwaListItemConverter(
                currentTrendInterval = request.value.interval,
                appCurrency = appCurrency.value,
                onItemClick = onItemClick,
            )

        override fun convert(item: TokenMarket) = internalConverter.convert(item)

        override fun update(prevDomain: TokenMarket, currentUI: RwaListItemUM, newDomain: TokenMarket) =
            internalConverter.update(prevDomain, currentUI, newDomain)
    }

    private val stateManager = BatchListStateManager<Int, TokenMarket, RwaListItemUM>(
        converter = batchConverter,
        dispatchers = dispatchers,
    )

    val uiItems: StateFlow<ImmutableList<RwaListItemUM>> = stateManager.state
        .map { state ->
            state.uiBatches.asSequence().flatMap { it.data }
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

    val isInInitialLoadingErrorState = batchFlow.state
        .map { it.status is PaginationStatus.InitialLoadingError }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    val initialLoadingError: Flow<Throwable> = batchFlow.state
        .map { it.status }
        .distinctUntilChanged()
        .filterIsInstance<PaginationStatus.InitialLoadingError>()
        .map { it.throwable }

    /** The source's own result count, which can exceed the number of batches loaded so far. */
    val totalCount: StateFlow<Int?> = batchFlow.state
        .map { it.totalCount }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    /**
     * True only once the source is exhausted with nothing in it, so an in-flight request never renders as
     * "not found".
     *
     * Deliberately does not consult [request] — a predicate reading a value that is not in this flow's
     * upstream only recomputes when the batch state happens to move, and so stays stale across the whole
     * gap between a new query and its reload. Callers that care about the query already gate on it.
     */
    val isEmptyResultState: StateFlow<Boolean> = batchFlow.state
        .map { it.status is PaginationStatus.EndOfPagination && it.data.isEmpty() }
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
    }

    fun updateUIWithSameState() {
        modelScope.launch(dispatchers.default) {
            val current = batchFlow.state.value.data
            stateManager.update(current, forceUpdate = true)
        }.saveIn(updateStateJob)
    }

    fun reload() {
        modelScope.launch {
            val currentRequest = request.value

            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = TokenMarketListConfig(
                        fiatPriceCurrency = appCurrency.value.code,
                        searchText = currentRequest.searchText,
                        priceChangeInterval = currentRequest.interval.toBatchRequestInterval(),
                        order = currentRequest.sort.toRequestOrder(),
                        categoryId = currentRequest.selection.category?.id,
                        sectorId = currentRequest.selection.sector?.id,
                        country = currentRequest.country,
                    ),
                ),
            )
        }
    }

    fun clearStateAndStopAllActions() {
        modelScope.launch {
            actionsFlow.emit(BatchAction.Reset)
        }
    }

    fun loadMore() {
        modelScope.launch {
            actionsFlow.emit(BatchAction.LoadMore())
        }
    }

    fun loadCharts(batchKeys: Set<Int>, interval: RwaInterval) {
        if (batchKeys.isEmpty()) return

        modelScope.launch {
            val currentData = batchFlow.state.value.data
            val alreadyLoadedChartsBatchKeys = currentData
                .filter { batch ->
                    val first = batch.data.firstOrNull() ?: return@filter false
                    val chartByInterval = when (interval) {
                        RwaInterval.H24 -> first.tokenCharts.h24
                        RwaInterval.D7 -> first.tokenCharts.week
                        RwaInterval.M1 -> first.tokenCharts.month
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
                            currency = appCurrency.value.code,
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
                        currencyId = appCurrency.value.code,
                    ),
                    async = true,
                    operationId = "update quotes",
                ),
            )
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
}