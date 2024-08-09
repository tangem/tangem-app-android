package com.tangem.features.markets.tokenlist.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenMarket
import com.tangem.features.markets.component.BottomSheetState
import com.tangem.features.markets.tokenlist.impl.model.statemanager.MarketsListUMStateManager
import com.tangem.features.markets.tokenlist.impl.model.statemanager.MarketsListBatchFlowManager
import com.tangem.features.markets.tokenlist.impl.ui.state.ListUM
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListItemUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByTypeUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60000L
private const val SEARCH_QUERY_DEBOUNCE_MILLIS = 800L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@ComponentScoped
@Stable
internal class MarketsListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
) : Model() {

    private var updateQuotesJob = JobHolder()

    private val currentAppCurrency = getSelectedAppCurrencyUseCase()
        .map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val visibleItemIds = MutableStateFlow<List<String>>(emptyList())

    private val marketsListUMStateManager = MarketsListUMStateManager(
        onLoadMoreUiItems = { activeListManager.loadMore() },
        visibleItemsChanged = { visibleItemIds.value = it },
        onRetryButtonClicked = { activeListManager.reload() },
        onTokenClick = { onTokenUIClicked(it) },
    )
    private val mainMarketsListManager = MarketsListBatchFlowManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
        currentAppCurrency = Provider { currentAppCurrency.value },
        currentTrendInterval = Provider { marketsListUMStateManager.selectedInterval },
        currentSortByType = Provider { marketsListUMStateManager.selectedSortByType },
        currentSearchText = Provider { null },
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    private val searchMarketsListManager = MarketsListBatchFlowManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
        currentAppCurrency = Provider { currentAppCurrency.value },
        currentTrendInterval = Provider { marketsListUMStateManager.selectedInterval }, // FIXME fix on backend
        currentSortByType = Provider { SortByTypeUM.Rating }, // FIXME maybe fix on backend
        currentSearchText = Provider { marketsListUMStateManager.searchQuery },
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    private var activeListManager: MarketsListBatchFlowManager = mainMarketsListManager

    private val _tokenSelected = MutableSharedFlow<Pair<TokenMarket, AppCurrency>>()

    val tokenSelected = _tokenSelected.asSharedFlow()

    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    val state = marketsListUMStateManager.state.asStateFlow()

    init {
        @Suppress("UnnecessaryParentheses")
        modelScope.launch {
            marketsListUMStateManager.isInSearchStateFlow
                .flatMapLatest { isInSearchMode ->
                    if (isInSearchMode) {
                        combine(
                            searchMarketsListManager.uiItems,
                            searchMarketsListManager.isInInitialLoadingErrorState,
                            searchMarketsListManager.isSearchNotFoundState,
                        ) { items, isError, notFound ->
                            (items to isError) to notFound
                        }
                    } else {
                        combine(
                            mainMarketsListManager.uiItems,
                            mainMarketsListManager.isInInitialLoadingErrorState,
                        ) { items, isError -> (items to isError) to false }
                    }
                }.collect {
                    marketsListUMStateManager.onUiItemsChanged(
                        uiItems = it.first.first,
                        isInErrorState = it.first.second,
                        isSearchNotFound = it.second,
                    )
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
                mainMarketsListManager.reload()
                if (marketsListUMStateManager.isInSearchState) {
                    searchMarketsListManager.reload()
                }
            }.launchIn(modelScope)

        // load charts when new batch is being loaded
        mainMarketsListManager.onLastBatchLoadedSuccess
            .onEach {
                mainMarketsListManager.loadCharts(setOf(it), marketsListUMStateManager.selectedInterval)
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
                            mainMarketsListManager.updateUIWithSameState()
                            val batchKeys = mainMarketsListManager.getBatchKeysByItemIds(visibleItemIds.value)
                            mainMarketsListManager.loadCharts(batchKeys, interval)
                        }
                        else -> mainMarketsListManager.reload()
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
                    mainMarketsListManager.reload()
                }
        }

        // listen current visible batch and update charts
        modelScope.launch {
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
// [REDACTED_TODO_COMMENT]
                    activeListManager.loadCharts(visibleBatchKeys, marketsListUMStateManager.selectedInterval)
                }
        }

        // ===Search===

        modelScope.launch {
            marketsListUMStateManager.isInSearchStateFlow
                .collectLatest { isInSearchMode ->
                    activeListManager = if (isInSearchMode) {
                        searchMarketsListManager
                    } else {
                        searchMarketsListManager.clearStateAndStopAllActions()
                        mainMarketsListManager
                    }
                }
        }

        modelScope.launch {
            marketsListUMStateManager.searchQueryFlow
                .debounce(timeoutMillis = SEARCH_QUERY_DEBOUNCE_MILLIS)
                .distinctUntilChanged()
                .onEach {
                    if (it.isEmpty()) searchMarketsListManager.clearStateAndStopAllActions()
                }
                .filter { it.isNotEmpty() && activeListManager == searchMarketsListManager }
                .collectLatest {
                    searchMarketsListManager.reload(searchText = it)
                }
        }

        modelScope.launch {
            searchMarketsListManager
                .onLastBatchLoadedSuccess
                .collectLatest {
                    searchMarketsListManager.loadCharts(setOf(it), marketsListUMStateManager.selectedInterval)
                    modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
                }
        }

        // initial loading
        mainMarketsListManager.reload()
    }

    private fun onTokenUIClicked(token: MarketsListItemUM) {
        modelScope.launch {
            activeListManager.getTokenById(token.id)?.let { found ->
                _tokenSelected.emit(found to currentAppCurrency.value)
            }
        }
    }

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                // Update quotes only when the container bottom sheet is in the expanded state
                containerBottomSheetState.first { it == BottomSheetState.EXPANDED }
                // and is visible on the screen
                isVisibleOnScreen.first { it }

                activeListManager.updateQuotes()
            }
        }.saveIn(updateQuotesJob)
    }
}
