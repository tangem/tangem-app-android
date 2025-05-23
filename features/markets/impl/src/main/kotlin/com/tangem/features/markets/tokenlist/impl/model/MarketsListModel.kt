package com.tangem.features.markets.tokenlist.impl.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.GetStakingNotificationMaxApyUseCase
import com.tangem.domain.markets.TokenMarket
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.promo.PromoRepository
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.settings.usercountry.models.UserCountryError
import com.tangem.domain.settings.usercountry.models.needApplyFCARestrictions
import com.tangem.features.markets.entry.BottomSheetState
import com.tangem.features.markets.tokenlist.impl.analytics.MarketsListAnalyticsEvent
import com.tangem.features.markets.tokenlist.impl.model.statemanager.MarketsListBatchFlowManager
import com.tangem.features.markets.tokenlist.impl.model.statemanager.MarketsListUMStateManager
import com.tangem.features.markets.tokenlist.impl.ui.state.ListUM
import com.tangem.features.markets.tokenlist.impl.ui.state.MarketsListItemUM
import com.tangem.features.markets.tokenlist.impl.ui.state.SortByTypeUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import javax.inject.Inject

private const val UPDATE_QUOTES_TIMER_MILLIS = 60000L
private const val SEARCH_QUERY_DEBOUNCE_MILLIS = 800L

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@ModelScoped
@Stable
@Suppress("LongParameterList")
internal class MarketsListModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getStakingNotificationMaxApyUseCase: GetStakingNotificationMaxApyUseCase,
    private val promoRepository: PromoRepository,
    private val getUserCountryUseCase: GetUserCountryUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private var updateQuotesJob = JobHolder()

    private val currentAppCurrency = getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
        maybeAppCurrency.getOrElse { AppCurrency.Default }
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppCurrency.Default,
    )

    private val visibleItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    private val marketsListUMStateManager = MarketsListUMStateManager(
        currentVisibleIds = Provider { visibleItemIds.value },
        onLoadMoreUiItems = { activeListManager.loadMore() },
        visibleItemsChanged = { visibleItemIds.value = it },
        onRetryButtonClicked = { activeListManager.reload() },
        onTokenClick = { onTokenUIClicked(it) },
        onStakingNotificationClick = { analyticsEventHandler.send(MarketsListAnalyticsEvent.StakingMoreInfoClicked) },
        onStakingNotificationCloseClick = { onStakingNotificationCloseClick() },
        onShowTokensUnder100kClicked = { analyticsEventHandler.send(MarketsListAnalyticsEvent.ShowTokens) },
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
        currentTrendInterval = Provider { marketsListUMStateManager.selectedInterval },
        currentSortByType = Provider { SortByTypeUM.Rating },
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
        @Suppress("UnnecessaryParentheses") modelScope.launch {
            marketsListUMStateManager.isInSearchStateFlow.flatMapLatest { isInSearchMode ->
                if (isInSearchMode) {
                    combine(
                        searchMarketsListManager.uiItems,
                        searchMarketsListManager.isInInitialLoadingErrorState,
                        searchMarketsListManager.isSearchNotFoundState,
                        getStakingNotificationMaxApyUseCase(),
                        getUserCountryUseCase.invoke(),
                    ) { uiItems, isInInitialLoadingErrorState, isSearchNotFoundState, stakingMaxApy, userCountry ->
                        MarketsItemsData(
                            items = uiItems,
                            isInErrorState = isInInitialLoadingErrorState,
                            isSearchNotFound = isSearchNotFoundState,
                            stakingNotificationMaxApy = stakingMaxApy,
                            userCountry = userCountry,
                        )
                    }
                } else {
                    combine(
                        mainMarketsListManager.uiItems,
                        mainMarketsListManager.isInInitialLoadingErrorState,
                        getStakingNotificationMaxApyUseCase(),
                        getUserCountryUseCase.invoke(),
                    ) { uiItems, isInInitialLoadingErrorState, stakingNotificationMaxApy, userCountry ->
                        MarketsItemsData(
                            items = uiItems,
                            isInErrorState = isInInitialLoadingErrorState,
                            isSearchNotFound = false,
                            stakingNotificationMaxApy = stakingNotificationMaxApy,
                            userCountry = userCountry,
                        )
                    }
                }
            }.collect { marketsItemsData ->
                val stakingNotificationMaxApy = marketsItemsData.stakingNotificationMaxApy?.takeUnless {
                    marketsItemsData.userCountry.getOrNull().needApplyFCARestrictions()
                }

                if (marketsListUMStateManager.state.value.stakingNotificationMaxApy == null &&
                    stakingNotificationMaxApy != null
                ) {
                    analyticsEventHandler.send(MarketsListAnalyticsEvent.StakingPromoShown)
                }

                marketsListUMStateManager.onUiItemsChanged(
                    uiItems = marketsItemsData.items,
                    isInErrorState = marketsItemsData.isInErrorState,
                    isSearchNotFound = marketsItemsData.isSearchNotFound,
                    stakingNotificationMaxApy = marketsItemsData.stakingNotificationMaxApy?.takeUnless {
                        marketsItemsData.userCountry.getOrNull().needApplyFCARestrictions()
                    },
                )
            }
        }

        state.onEach {
            if (it.list !is ListUM.Content) {
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
        mainMarketsListManager.onLastBatchLoadedSuccess.onEach {
            mainMarketsListManager.loadCharts(setOf(it), marketsListUMStateManager.selectedInterval)
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
            marketsListUMStateManager.state.map { it.selectedSortBy }.distinctUntilChanged().drop(1).collectLatest {
                mainMarketsListManager.reload()
            }
        }

        // listen current visible batch and update charts
        modelScope.launch {
            visibleItemIds.mapNotNull {
                if (it.isNotEmpty()) {
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
                activeListManager = if (isInSearchMode) {
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
            searchMarketsListManager.onLastBatchLoadedSuccess.collectLatest {
                searchMarketsListManager.loadCharts(setOf(it), marketsListUMStateManager.selectedInterval)
                modelScope.loadQuotesWithTimer(timeMillis = UPDATE_QUOTES_TIMER_MILLIS)
            }
        }

        searchMarketsListManager.isSearchNotFoundState.onEach {
            if (it) {
                analyticsEventHandler.send(MarketsListAnalyticsEvent.TokenSearched(tokenFound = false))
            }
        }.launchIn(modelScope)

        searchMarketsListManager.onFirstBatchLoadedSuccess.onEach {
            analyticsEventHandler.send(MarketsListAnalyticsEvent.TokenSearched(tokenFound = true))
        }.launchIn(modelScope)

        // analytics
        initAnalytics()

        // initial loading
        mainMarketsListManager.reload()
    }

    private fun initAnalytics() {
        containerBottomSheetState.onEach {
            if (it == BottomSheetState.EXPANDED) {
                analyticsEventHandler.send(MarketsListAnalyticsEvent.BottomSheetOpened)
            }
        }.launchIn(modelScope)

        state.filter { it.isInSearchMode.not() }
            .map { MarketsListAnalyticsEvent.SortBy(it.selectedSortBy, it.selectedInterval) }.distinctUntilChanged()
            .onEach {
                analyticsEventHandler.send(it)
            }.launchIn(modelScope)
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

    private fun onStakingNotificationCloseClick() {
        analyticsEventHandler.send(MarketsListAnalyticsEvent.StakingPromoClosed)
        modelScope.launch {
            promoRepository.setMarketsStakingNotificationHideClicked()
        }
    }

    private class MarketsItemsData(
        val items: ImmutableList<MarketsListItemUM>,
        val isInErrorState: Boolean,
        val isSearchNotFound: Boolean,
        val stakingNotificationMaxApy: BigDecimal?,
        val userCountry: Either<UserCountryError, UserCountry>,
    )
}