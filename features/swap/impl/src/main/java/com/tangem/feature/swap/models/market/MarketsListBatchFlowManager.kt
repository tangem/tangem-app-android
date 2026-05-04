package com.tangem.feature.swap.models.market

import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.models.market.converter.SwapMarketsTokenItemConverter
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class MarketsListBatchFlowManager @AssistedInject constructor(
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    @Assisted private val batchFlowType: GetMarketsTokenListFlowUseCase.BatchFlowType,
    @Assisted private val order: TokenMarketListConfig.Order,
    @Assisted private val currentSearchText: Provider<String?>,
    @Assisted private val modelScope: CoroutineScope,
) {
    private val actionsFlow =
        MutableSharedFlow<BatchAction<Int, TokenMarketListConfig, TokenMarketUpdateRequest>>(replay = 1)
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

    private val appCurrency: StateFlow<AppCurrency> = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(modelScope, SharingStarted.Eagerly, AppCurrency.Default)

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

    val totalCount: StateFlow<Int?> = batchFlow.state
        .map { it.totalCount }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
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

                val converter = SwapMarketsTokenItemConverter(appCurrency = appCurrency.value)

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
                        fiatPriceCurrency = appCurrency.value.code,
                        searchText = if (currentSearchText() == null) {
                            null
                        } else {
                            searchText ?: currentSearchText()
                        },
                        priceChangeInterval = TokenMarketListConfig.Interval.H24,
                        order = order,
                        shouldNetworks = true,
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
                            currency = appCurrency.value.code,
                        ),
                        async = true,
                        operationId = batchesKeysToLoad.toString() + "h24",
                    ),
                )
            }
        }
    }

    fun getBatchKeysByItemIds(ids: List<CryptoCurrency.RawID>): Set<Int> {
        val currentData = batchFlow.state.value.data

        return currentData
            .filter { d -> d.data.any { ids.contains(it.id) } }
            .map { it.key }
            .toSet()
    }

    fun getTokenMarketById(id: CryptoCurrency.RawID): TokenMarket? {
        return batchFlow.state.value.data
            .asSequence()
            .flatMap { it.data }
            .firstOrNull { it.id == id }
    }

    private data class ResultBatches(
        val uiBatches: List<Batch<Int, List<MarketsListItemUM>>> = emptyList(),
        val processedItems: List<Batch<Int, List<TokenMarket>>>? = null,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            batchFlowType: GetMarketsTokenListFlowUseCase.BatchFlowType,
            order: TokenMarketListConfig.Order,
            currentSearchText: Provider<String?>,
            modelScope: CoroutineScope,
        ): MarketsListBatchFlowManager
    }
}