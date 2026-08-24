package com.tangem.features.feed.crypto.model.list

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenListBatchingContext
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.TokenMarketUpdateRequest
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.feed.crypto.ui.state.MarketPulseItemUM
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
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
 * Paginated markets list, used both for the Market Pulse block and for the Crypto tab's search
 * results. Port of the old feed's `MarketsListBatchFlowManager`.
 *
 * Search mode is one instance with [batchFlowType] `Search` and a non-null [currentSearchText]; the two
 * modes never share an instance, because their request configs differ and a single batch flow would
 * thrash between them.
 */
@Suppress("LongParameterList")
internal class MarketPulseBatchFlowManager(
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val currentTrendInterval: () -> MarketPulseInterval,
    private val currentAppCurrency: () -> AppCurrency,
    private val currentCategory: () -> MarketPulseCategory,
    private val onItemClick: (CryptoCurrency.RawID) -> Unit,
    private val modelScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    batchFlowType: GetMarketsTokenListFlowUseCase.BatchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
    private val currentSearchText: () -> String? = { null },
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

    private val batchConverter = object : BatchItemConverter<TokenMarket, MarketPulseItemUM> {

        private val internalConverter: MarketPulseItemConverter
            get() = MarketPulseItemConverter(
                currentTrendInterval = currentTrendInterval(),
                appCurrency = currentAppCurrency(),
                onItemClick = onItemClick,
            )

        override fun convert(item: TokenMarket) = internalConverter.convert(item)

        override fun update(prevDomain: TokenMarket, currentUI: MarketPulseItemUM, newDomain: TokenMarket) =
            internalConverter.update(prevDomain, currentUI, newDomain)
    }

    private val stateManager = BatchListStateManager<Int, TokenMarket, MarketPulseItemUM>(
        converter = batchConverter,
        dispatchers = dispatchers,
    )

    val uiItems: StateFlow<ImmutableList<MarketPulseItemUM>> = stateManager.state
        .map { state ->
            state.uiBatches.asSequence()
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

    /** True only once the source is exhausted, so an in-flight search never renders as "not found". */
    val isSearchNotFoundState: StateFlow<Boolean> = batchFlow.state
        .map { state ->
            currentSearchText().isNullOrEmpty().not() &&
                state.status is PaginationStatus.EndOfPagination &&
                state.data.isEmpty()
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
    }

    /** Re-runs the converter over the current domain data (e.g. after an interval switch). */
    fun updateUIWithSameState() {
        modelScope.launch(dispatchers.default) {
            val current = batchFlow.state.value.data
            stateManager.update(current, forceUpdate = true)
        }.saveIn(updateStateJob)
    }

    fun reload(searchText: String? = null) {
        modelScope.launch {
            actionsFlow.emit(
                BatchAction.Reload(
                    requestParams = TokenMarketListConfig(
                        fiatPriceCurrency = currentAppCurrency().code,
                        // a search text is ignored outside search mode, so the Market Pulse list cannot be
                        // narrowed by a stray argument
                        searchText = currentSearchText()?.let { current -> searchText ?: current },
                        priceChangeInterval = currentTrendInterval().toBatchRequestInterval(),
                        order = currentCategory().toRequestOrder(),
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

    fun loadCharts(batchKeys: Set<Int>, interval: MarketPulseInterval) {
        if (batchKeys.isEmpty()) return

        modelScope.launch {
            val currentData = batchFlow.state.value.data
            val alreadyLoadedChartsBatchKeys = currentData
                .filter { batch ->
                    val first = batch.data.firstOrNull() ?: return@filter false
                    val chartByInterval = when (interval) {
                        MarketPulseInterval.H24 -> first.tokenCharts.h24
                        MarketPulseInterval.D7 -> first.tokenCharts.week
                        MarketPulseInterval.M1 -> first.tokenCharts.month
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