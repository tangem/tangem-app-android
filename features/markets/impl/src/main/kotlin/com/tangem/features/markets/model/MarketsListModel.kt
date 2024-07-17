package com.tangem.features.markets.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.model.statemanager.MarketsListUMStateManager
import com.tangem.features.markets.model.statemanager.MarketsListUiItemsManager
import com.tangem.features.markets.ui.entity.ListUM
import com.tangem.features.markets.ui.entity.SortByTypeUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60000L

@ComponentScoped
@Stable
internal class MarketsListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : Model() {

    private val currentAppCurrency = getSelectedAppCurrencyUseCase()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val visibleItemIds = MutableStateFlow<List<String>>(emptyList())
    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    private val marketsListUMStateManager = MarketsListUMStateManager(
        onLoadMoreUiItems = { activeListManager.loadMore() },
        visibleItemsChanged = { visibleItemIds.value = it },
    )

    private val marketsListManager = MarketsListUiItemsManager(
        logTag = "main",
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        currentAppCurrency = Provider { currentAppCurrency.value },
        currentTrendInterval = Provider { marketsListUMStateManager.selectedInterval },
        modelScope = modelScope,
        dispatchers = dispatchers,
    )
    private val searchMarketsListManager = MarketsListUiItemsManager(
        logTag = "search",
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        currentAppCurrency = Provider { currentAppCurrency.value },
        currentTrendInterval = Provider { marketsListUMStateManager.selectedInterval },
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    private var activeListManager: MarketsListUiItemsManager = marketsListManager

    val state = marketsListUMStateManager.state.asStateFlow()

    init {
        modelScope.launch {
            marketsListManager.uiItems
                .collectLatest {
                    marketsListUMStateManager.onUiItemsChanged(it)
                }
        }

        state.onEach {
            if (it.list !is ListUM.Content) {
                visibleItemIds.value = emptyList()
            }
        }.launchIn(modelScope)

        // update all lists when user's currency has changed
        currentAppCurrency
            .drop(1)
            .onEach {
                marketsListManager.reload(
                    interval = marketsListUMStateManager.selectedInterval,
                    sortBy = marketsListUMStateManager.selectedSortByType,
                )
                if (marketsListUMStateManager.isInSearchState) {
                    // TODO
                    searchMarketsListManager.reload(
                        interval = marketsListUMStateManager.selectedInterval,
                        sortBy = marketsListUMStateManager.selectedSortByType,
                    )
                }
            }.launchIn(modelScope)

        // load charts when new batch is being loaded
        marketsListManager.onLastBatchLoadedSuccess
            .onEach {
                marketsListManager.loadCharts(setOf(it), marketsListUMStateManager.selectedInterval)
                modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
            }
            .launchIn(modelScope)

        // listen currently selected interval, update charts if sorting=rating, or reload all list
        modelScope.launch(dispatchers.default) {
            marketsListUMStateManager.state
                .map { it.selectedInterval }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { interval ->
                    when (marketsListUMStateManager.selectedSortByType) {
                        SortByTypeUM.Rating -> {
                            marketsListManager.updateUIWithSameState()
                            val batchKeys = marketsListManager.getBatchKeysByItemIds(visibleItemIds.value)
                            marketsListManager.loadCharts(batchKeys, interval)
                        }
                        else -> marketsListManager.reload(interval, marketsListUMStateManager.selectedSortByType)
                    }
                }
        }

        // reload list when sorting type has changed
        modelScope.launch {
            marketsListUMStateManager.state
                .map { it.selectedSortBy }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest {
                    marketsListManager.reload(marketsListUMStateManager.selectedInterval, it)
                }
        }

        // listen current visible batch and update charts
        modelScope.launch(dispatchers.default) {
            visibleItemIds
                .mapNotNull {
                    if (it.isNotEmpty()) {
                        activeListManager.getBatchKeysByItemIds(visibleItemIds.value)
                    } else {
                        null
                    }
                }
                .distinctUntilChanged()
                .collectLatest { visibleBatchKeys ->
                    activeListManager.loadCharts(visibleBatchKeys, marketsListUMStateManager.selectedInterval)
                }
        }

        // initial loading
        marketsListManager.reload(
            interval = marketsListUMStateManager.selectedInterval,
            sortBy = marketsListUMStateManager.selectedSortByType,
        )
    }

    private var updateQuotesJob = JobHolder()
    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                // Update quotes only when the container bottom sheet is in the expanded state
                containerBottomSheetState.first { it == BottomSheetState.EXPANDED }
                activeListManager.updateQuotes()
            }
        }.saveIn(updateQuotesJob)
    }
}