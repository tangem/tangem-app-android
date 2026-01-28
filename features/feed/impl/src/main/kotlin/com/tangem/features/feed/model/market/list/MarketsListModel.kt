package com.tangem.features.feed.model.market.list

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.ShouldShowYieldModeMarketPromoUseCase
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.UserCountryError
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.features.feed.components.market.list.DefaultMarketsTokenListComponent
import com.tangem.features.feed.model.market.list.analytics.MarketsListAnalyticsEvent
import com.tangem.features.feed.model.market.list.state.ListUM
import com.tangem.features.feed.model.market.list.state.MarketsListUM
import com.tangem.features.feed.model.market.list.state.MarketsNotificationUM
import com.tangem.features.feed.model.market.list.state.SortByTypeUM
import com.tangem.features.feed.model.market.list.statemanager.MarketsListBatchFlowManager
import com.tangem.features.feed.model.market.list.statemanager.MarketsListUMStateManager
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60000L
private const val SEARCH_QUERY_DEBOUNCE_MILLIS = 800L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@ModelScoped
@Stable
@Suppress("LongParameterList", "PropertyUsedBeforeDeclaration")
internal class MarketsListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    shouldShowYieldModeMarketPromoUseCase: ShouldShowYieldModeMarketPromoUseCase,
    paramsContainer: ParamsContainer,
    private val promoRepository: PromoRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getUserCountryUseCase: GetUserCountryUseCase,
) : Model() {

    private val updateQuotesJob = JobHolder()

    private val params = paramsContainer.require<DefaultMarketsTokenListComponent.Params>()

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    private val visibleItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    private val marketsListUMStateManager by lazy {
        MarketsListUMStateManager(
            currentVisibleIds = Provider { visibleItemIds.value },
            onLoadMoreUiItems = { activeListManager.loadMore() },
            visibleItemsChanged = { visibleItemIds.value = it },
            onRetryButtonClicked = { activeListManager.reload() },
            onTokenClick = { onTokenUIClicked(it) },
            onShowTokensUnder100kClicked = { analyticsEventHandler.send(MarketsListAnalyticsEvent.ShowTokens()) },
            shouldAlwaysShowSearchBar = Provider { params.shouldAlwaysShowSearchBar },
            preselectedSortType = Provider { params.preselectedSortType },
            onBackClick = params.onBackClicked,
            analyticsEventHandler = analyticsEventHandler,
        )
    }

    private val mainMarketsListManager by lazy {
        MarketsListBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
            currentAppCurrency = Provider { currentAppCurrency.value },
            currentTrendInterval = Provider { marketsListUMStateManager.selectedInterval },
            currentSortByType = Provider { marketsListUMStateManager.selectedSortByType },
            currentSearchText = Provider { null },
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    private val searchMarketsListManager by lazy {
        MarketsListBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
            currentAppCurrency = Provider { currentAppCurrency.value },
            currentTrendInterval = Provider { marketsListUMStateManager.selectedInterval },
            currentSortByType = Provider { SortByTypeUM.Rating },
            currentSearchText = Provider { marketsListUMStateManager.searchQuery },
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    private var activeListManager: MarketsListBatchFlowManager = mainMarketsListManager

    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    val state = marketsListUMStateManager.state.asStateFlow()

    init {
        modelScope.launch {
            marketsListUMStateManager.isInSearchStateFlow.flatMapLatest { isInSearchMode ->
                if (isInSearchMode) {
                    combine(
                        flow = searchMarketsListManager.uiItems,
                        flow2 = searchMarketsListManager.isInInitialLoadingErrorState,
                        flow3 = searchMarketsListManager.isSearchNotFoundState,
                        flow4 = shouldShowYieldModeMarketPromoUseCase(
                            appCurrency = currentAppCurrency.value,
                            interval = marketsListUMStateManager.selectedInterval.toBatchRequestInterval(),
                        ).conflate(),
                        flow5 = getUserCountryUseCase.invoke(),
                    ) { uiItems, isInInitialLoadingErrorState, isSearchNotFoundState, isYieldModePromo, userCountry ->
                        MarketsItemsData(
                            items = uiItems,
                            isInErrorState = isInInitialLoadingErrorState,
                            isSearchNotFound = isSearchNotFoundState,
                            shouldShowYieldModePromo = isYieldModePromo,
                            userCountry = userCountry,
                        )
                    }
                } else {
                    combine(
                        flow = mainMarketsListManager.uiItems,
                        flow2 = mainMarketsListManager.isInInitialLoadingErrorState,
                        flow3 = shouldShowYieldModeMarketPromoUseCase(
                            appCurrency = currentAppCurrency.value,
                            interval = marketsListUMStateManager.selectedInterval.toBatchRequestInterval(),
                        ).conflate(),
                        flow4 = getUserCountryUseCase.invoke(),
                    ) { uiItems, isInInitialLoadingErrorState, shouldShowYieldModePromo, userCountry ->
                        MarketsItemsData(
                            items = uiItems,
                            isInErrorState = isInInitialLoadingErrorState,
                            isSearchNotFound = false,
                            shouldShowYieldModePromo = shouldShowYieldModePromo,
                            userCountry = userCountry,
                        )
                    }
                }
            }.collect { marketsItemsData ->
                val isApplyFCARestrictions = marketsItemsData.userCountry.getOrNull().needApplyFCARestrictions()
                val shouldShowYieldModePromo = marketsItemsData.shouldShowYieldModePromo && !isApplyFCARestrictions
                if (marketsListUMStateManager.state.value.marketsNotificationUM == null && shouldShowYieldModePromo) {
                    analyticsEventHandler.send(MarketsListAnalyticsEvent.YieldModePromoShown())
                }

                marketsListUMStateManager.onUiItemsChanged(
                    uiItems = marketsItemsData.items,
                    isInErrorState = marketsItemsData.isInErrorState,
                    isSearchNotFound = marketsItemsData.isSearchNotFound,
                    marketsNotificationUM = if (shouldShowYieldModePromo) {
                        MarketsNotificationUM.YieldSupplyPromo(
                            onClick = {
                                analyticsEventHandler.send(MarketsListAnalyticsEvent.YieldModeMoreInfoClicked())
                                marketsListUMStateManager.selectedSortByType = SortByTypeUM.YieldSupply
                            },
                            onCloseClick = { onYieldModeNotificationCloseClick() },
                        )
                    } else {
                        null
                    },
                )
            }
        }

        state.onEach { marketsListUM ->
            if (marketsListUM.list !is ListUM.Content) {
                visibleItemIds.value = emptyList()
            }
        }.launchIn(modelScope)

        // update all lists when user's currency has changed
        currentAppCurrency.drop(1).onEach {
            mainMarketsListManager.reload()
            if (marketsListUMStateManager.isInSearchState) {
                searchMarketsListManager.reload()
            }
        }.launchIn(modelScope)

        // load charts when new batch is being loaded
        mainMarketsListManager.onLastBatchLoadedSuccess.onEach { batchKey ->
            mainMarketsListManager.loadCharts(setOf(batchKey), marketsListUMStateManager.selectedInterval)
            modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)

        // listen currently selected interval, update charts if sorting=rating, or reload all list
        modelScope.launch(dispatchers.default) {
            marketsListUMStateManager.state.map { it.selectedInterval }.distinctUntilChanged().drop(1)
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
            marketsListUMStateManager
                .state
                .map { it.selectedSortBy }
                .distinctUntilChanged()
                .drop(1)
                .collectLatest { _ ->
                    mainMarketsListManager.reload()
                }
        }

        // listen current visible batch and update charts
        modelScope.launch {
            visibleItemIds.mapNotNull { listOfIds ->
                if (listOfIds.isNotEmpty()) {
                    activeListManager.getBatchKeysByItemIds(visibleItemIds.value)
                } else {
                    null
                }
            }.distinctUntilChanged().collectLatest { visibleBatchKeys ->
                // TODO load batch on scroll heat area
                activeListManager.loadCharts(visibleBatchKeys, marketsListUMStateManager.selectedInterval)
            }
        }

        // ===Search===

        modelScope.launch {
            marketsListUMStateManager.isInSearchStateFlow.collectLatest { isInSearchMode ->
                activeListManager = if (isInSearchMode || marketsListUMStateManager.searchQuery.isNotEmpty()) {
                    searchMarketsListManager
                } else {
                    searchMarketsListManager.clearStateAndStopAllActions()
                    mainMarketsListManager
                }
            }
        }

        modelScope.launch {
            marketsListUMStateManager.searchQueryFlow.debounce(timeoutMillis = SEARCH_QUERY_DEBOUNCE_MILLIS)
                .distinctUntilChanged().onEach {
                    if (it.isEmpty()) searchMarketsListManager.clearStateAndStopAllActions()
                }.filter { it.isNotEmpty() && activeListManager == searchMarketsListManager }.collectLatest {
                    searchMarketsListManager.reload(searchText = it)
                }
        }

        modelScope.launch {
            searchMarketsListManager.onLastBatchLoadedSuccess.collectLatest { batchKey ->
                searchMarketsListManager.loadCharts(setOf(batchKey), marketsListUMStateManager.selectedInterval)
                modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
            }
        }

        // analytics
        initAnalytics()

        // initial loading
        mainMarketsListManager.reload()
    }

    private fun MarketsListUM.TrendInterval.toBatchRequestInterval(): TokenMarketListConfig.Interval {
        return when (this) {
            MarketsListUM.TrendInterval.H24 -> TokenMarketListConfig.Interval.H24
            MarketsListUM.TrendInterval.D7 -> TokenMarketListConfig.Interval.WEEK
            MarketsListUM.TrendInterval.M1 -> TokenMarketListConfig.Interval.MONTH
        }
    }

    private fun initAnalytics() {
        searchMarketsListManager
            .isSearchNotFoundState
            .onEach { isTokenFound ->
                if (isTokenFound) {
                    analyticsEventHandler.send(MarketsListAnalyticsEvent.TokenSearched(isTokenFound = false))
                }
            }.launchIn(modelScope)

        searchMarketsListManager.onFirstBatchLoadedSuccess.onEach {
            analyticsEventHandler.send(MarketsListAnalyticsEvent.TokenSearched(isTokenFound = true))
        }.launchIn(modelScope)

        state
            .map { MarketsListAnalyticsEvent.SortBy(it.selectedSortBy, it.selectedInterval) }.distinctUntilChanged()
            .onEach {
                analyticsEventHandler.send(it)
            }.launchIn(modelScope)
    }

    private fun onTokenUIClicked(token: MarketsListItemUM) {
        modelScope.launch {
            activeListManager.getTokenById(token.id)?.let { found ->
                params.onTokenClick(found.toSerializableParam(), currentAppCurrency.value)
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

    private fun onYieldModeNotificationCloseClick() {
        analyticsEventHandler.send(MarketsListAnalyticsEvent.YieldModePromoClosed())
        modelScope.launch {
            promoRepository.setMarketsYieldSupplyNotificationHideClicked()
        }
    }

    private class MarketsItemsData(
        val items: ImmutableList<MarketsListItemUM>,
        val isInErrorState: Boolean,
        val isSearchNotFound: Boolean,
        val shouldShowYieldModePromo: Boolean,
        val userCountry: Either<UserCountryError, UserCountry>,
    )
}