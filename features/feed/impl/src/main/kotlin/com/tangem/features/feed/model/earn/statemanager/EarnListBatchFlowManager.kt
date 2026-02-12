package com.tangem.features.feed.model.earn.statemanager

import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.earn.usecase.GetEarnTokensBatchFlowUseCase
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.features.feed.model.converter.EarnTokenWithCurrencyToListItemUMConverter
import com.tangem.features.feed.model.earn.analytics.BEST_OPPORTUNITIES_SOURCE
import com.tangem.features.feed.ui.earn.state.EarnListItemUM
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

internal class EarnListBatchFlowManager(
    getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase,
    private val configProvider: Provider<EarnTokensListConfig>,
    private val onItemClick: (EarnTokenWithCurrency, source: String) -> Unit,
    private val modelScope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    private val actionsFlow = MutableSharedFlow<BatchAction<Int, EarnTokensListConfig, Nothing>>()
    private val converter = EarnTokenWithCurrencyToListItemUMConverter(
        onItemClick = { onItemClick(it, BEST_OPPORTUNITIES_SOURCE) },
    )

    private val batchFlow = getEarnTokensBatchFlowUseCase(
        context = EarnTokensBatchingContext(
            actionsFlow = actionsFlow,
            coroutineScope = modelScope,
        ),
        batchSize = DEFAULT_BATCH_SIZE,
    )

    val uiItems: StateFlow<ImmutableList<EarnListItemUM>> =
        batchFlow.state
            .scan(persistentListOf<EarnListItemUM>() to -1) { (accItems, lastProcessedBatchIndex), newState ->
                if (newState.data.size <= lastProcessedBatchIndex) {
                    val newItems = newState.data
                        .flatMap { it.data }
                        .map(converter::convert)
                        .toPersistentList()
                    newItems to newState.data.lastIndex
                } else {
                    val newBatches = newState.data.subList(lastProcessedBatchIndex + 1, newState.data.size)
                    val newItems = newBatches
                        .flatMap { it.data }
                        .map(converter::convert)
                    accItems.addAll(newItems) to newState.data.lastIndex
                }
            }
            .map { (items, _) -> items }
            .distinctUntilChanged()
            .stateIn(
                scope = modelScope,
                started = SharingStarted.Eagerly,
                initialValue = persistentListOf(),
            )

    val initialLoadingError: StateFlow<Throwable?> = batchFlow.state
        .map { state ->
            val status = state.status
            if (status is PaginationStatus.InitialLoadingError) {
                status.throwable
            } else {
                null
            }
        }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

    val paginationStatus: StateFlow<PaginationStatus<List<EarnTokenWithCurrency>>> = batchFlow.state
        .map { it.status }
        .distinctUntilChanged()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = PaginationStatus.InitialLoading,
        )

    fun reload() {
        modelScope.launch(dispatchers.default) {
            actionsFlow.emit(
                BatchAction.Reload(requestParams = configProvider()),
            )
        }
    }

    fun loadMore() {
        modelScope.launch(dispatchers.default) {
            actionsFlow.emit(BatchAction.LoadMore())
        }
    }

    private companion object {
        private const val DEFAULT_BATCH_SIZE = 20
    }
}