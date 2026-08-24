package com.tangem.features.feed.crypto.model.search

import com.tangem.common.routing.AppRoute
import com.tangem.common.ui.markets.tokenselector.UserAssetEntrySectionsConverter
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.feed.crypto.CryptoFeedSearchTabComponent
import com.tangem.features.feed.crypto.model.analytics.CryptoSearchAnalyticsEvent
import com.tangem.features.feed.crypto.model.list.MarketPulseBatchFlowManager
import com.tangem.features.feed.crypto.model.list.MarketPulseCategory
import com.tangem.features.feed.crypto.model.list.MarketPulseInterval
import com.tangem.features.feed.crypto.ui.state.CryptoSearchUM
import com.tangem.features.feed.crypto.ui.state.MarketSearchUM
import com.tangem.features.feed.crypto.ui.state.PortfolioSearchUM
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MARKET_SEARCH_DEBOUNCE_MS = 500L
private const val UPDATE_QUOTES_TIMER_MILLIS = 60_000L
private const val RESULTS_SHOWN_DEBOUNCE_MS = 1_000L

@Suppress("LongParameterList", "TooManyFunctions")
@ModelScoped
internal class CryptoFeedSearchTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    userWalletsListRepository: UserWalletsListRepository,
    private val getSearchResultsUseCase: GetSearchResultsUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val router: Router,
) : Model() {

    private val query = paramsContainer.require<CryptoFeedSearchTabComponent.Params>().query

    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    val uiState: StateFlow<CryptoSearchUM>
        field = MutableStateFlow(initialCryptoSearchState())

    private val updateQuotesJob = JobHolder()

    // part of the market pipeline rather than a var read from inside it, so the flag and the results it
    // filters can never disagree
    private val shouldShowAllTokens = MutableStateFlow(false)

    private val portfolioResultsCount = MutableStateFlow(0)

    private val currentAppCurrency = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )

    private val isBalanceHidden = getBalanceHidingSettingsUseCase.isBalanceHidden()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    private val walletIcons = userWalletsListRepository.userWallets
        .filterNotNull()
        .map { wallets ->
            wallets.associate { wallet ->
                wallet.walletId to walletIconUMConverter.convert(getWalletIconUseCase(wallet))
            }
        }
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap(),
        )

    private val isOnScreen: StateFlow<Boolean> = combine(
        flow = isVisibleOnScreen,
        flow2 = containerBottomSheetState,
    ) { isVisible, sheetState ->
        isVisible && sheetState == BottomSheetState.EXPANDED
    }.stateIn(
        scope = modelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val listManager = MarketPulseBatchFlowManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        // search is fixed at the legacy scope: 24h trend, rating order
        currentTrendInterval = { MarketPulseInterval.H24 },
        currentAppCurrency = { currentAppCurrency.value },
        currentCategory = { MarketPulseCategory.MarketCap },
        onItemClick = ::openTokenDetails,
        modelScope = modelScope,
        dispatchers = dispatchers,
        batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
        currentSearchText = { query.value.trim() },
    )

    init {
        modelScope.launch {
            // the whole pager is built when the search screen opens, so no request may leave before the
            // user both selected this tab and typed something
            isOnScreen.first { it }
            query
                .map(String::trim)
                .distinctUntilChanged()
                .collectLatest(::onQueryChanged)
        }

        subscribeToMarketItems()
        subscribeToAppCurrencyChanges()
        subscribeToResultsShown()
        subscribeToMarketLoadingErrors()

        listManager.onLastBatchLoadedSuccess.onEach { batchKey ->
            listManager.loadCharts(setOf(batchKey), MarketPulseInterval.H24)
            modelScope.loadQuotesWithTimer(UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)
    }

    private suspend fun onQueryChanged(query: String) {
        shouldShowAllTokens.value = false
        portfolioResultsCount.value = 0

        if (query.isEmpty()) {
            listManager.clearStateAndStopAllActions()
            updateQuotesJob.cancel()
            uiState.value = initialCryptoSearchState()
            return
        }

        uiState.update { it.startingNewQuery(query) }

        // collectLatest cancels this scope on the next keystroke, which both drops the pending reload and
        // stops the superseded portfolio subscription
        coroutineScope {
            launch { observePortfolio(query) }
            launch {
                delay(MARKET_SEARCH_DEBOUNCE_MS)
                listManager.reload(searchText = query)
            }
        }
    }

    private suspend fun observePortfolio(query: String) {
        combine(
            flow = getSearchResultsUseCase(query = query),
            flow2 = currentAppCurrency,
            flow3 = isBalanceHidden,
            flow4 = walletIcons,
        ) { result, appCurrency, balanceHidden, icons ->
            val entries = result.userAssets.toEntries()
            val sections = UserAssetEntrySectionsConverter(
                appCurrency = appCurrency,
                isBalanceHidden = balanceHidden,
                walletIcons = icons,
                onEntryClick = ::openPortfolioToken,
            ).convert(entries)

            entries.size to sections
        }.collectLatest { (entriesCount, sections) ->
            val portfolio = if (sections.isEmpty()) {
                PortfolioSearchUM.Empty
            } else {
                PortfolioSearchUM.Content(sections)
            }
            portfolioResultsCount.value = entriesCount
            uiState.update { state -> state.copy(portfolio = portfolio) }
        }
    }

    private fun subscribeToMarketItems() {
        combine(
            flow = query.map(String::trim).distinctUntilChanged(),
            flow2 = listManager.uiItems,
            flow3 = listManager.isSearchNotFoundState,
            flow4 = listManager.isInInitialLoadingErrorState,
            flow5 = shouldShowAllTokens,
        ) { currentQuery, items, isSearchNotFound, isInErrorState, showAllTokens ->
            if (currentQuery.isEmpty()) return@combine null

            when {
                isInErrorState -> MarketSearchUM.Error(onRetry = ::retry)
                isSearchNotFound -> MarketSearchUM.NotFound
                items.isEmpty() -> MarketSearchUM.Loading
                else -> buildMarketContent(
                    items = items,
                    shouldShowAllTokens = showAllTokens,
                    onShowAllTokens = ::showAllTokens,
                    loadMore = listManager::loadMore,
                )
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { snapshot -> uiState.update { it.applyMarketSnapshot(snapshot) } }
            .launchIn(modelScope)
    }

    private fun subscribeToAppCurrencyChanges() {
        modelScope.launch {
            // the same gate as the query collector: a change arriving while the user reads another tab
            // waits until this one is on screen instead of firing a request from behind it
            currentAppCurrency.drop(1).collectLatest { _ ->
                isOnScreen.first { it }
                if (query.value.isNotBlank()) listManager.reload()
            }
        }
    }

    /**
     * Counts are what makes this event the tab's rather than the host's, and the host has neither.
     * One event per query, once both sides have settled.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun subscribeToResultsShown() {
        query.map(String::trim)
            .distinctUntilChanged()
            .flatMapLatest { currentQuery ->
                if (currentQuery.isEmpty()) {
                    emptyFlow()
                } else {
                    combine(
                        flow = listManager.totalCount.filterNotNull(),
                        flow2 = portfolioResultsCount,
                    ) { marketCount, portfolioCount -> marketCount to portfolioCount }
                        .debounce(RESULTS_SHOWN_DEBOUNCE_MS)
                        .take(1)
                }
            }
            .onEach { (marketCount, portfolioCount) ->
                analyticsEventHandler.send(
                    CryptoSearchAnalyticsEvent.ResultsShown(
                        totalResultsCount = marketCount + portfolioCount,
                        marketsResultsCount = marketCount,
                        userTokensResultsCount = portfolioCount,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    private fun subscribeToMarketLoadingErrors() {
        listManager.initialLoadingError
            .onEach { throwable ->
                val code = (throwable as? ApiResponseError.HttpException)?.code?.numericCode
                analyticsEventHandler.send(
                    CryptoSearchAnalyticsEvent.ErrorMarketsData(code = code, message = throwable.message.orEmpty()),
                )
            }
            .launchIn(modelScope)
    }

    private fun retry() {
        listManager.reload()
    }

    private fun showAllTokens() {
        shouldShowAllTokens.value = true
    }

    private fun openPortfolioToken(entry: UserAssetEntry) {
        analyticsEventHandler.send(
            CryptoSearchAnalyticsEvent.PortfolioItemClicked(entry.currencyStatus.currency.symbol),
        )
        // not a FeedRoute: FeedRouter falls through to the app router for these
        router.push(
            AppRoute.CurrencyDetails(
                userWalletId = entry.userWalletId,
                currency = entry.currencyStatus.currency,
            ),
        )
    }

    private fun openTokenDetails(id: CryptoCurrency.RawID) {
        val token = listManager.getTokenById(id) ?: return
        analyticsEventHandler.send(CryptoSearchAnalyticsEvent.MarketItemClicked(token.symbol))
        router.push(
            route = FeedRoute.MarketsTokenDetails(
                token = token.toSerializableParam(),
                appCurrency = currentAppCurrency.value,
                shouldShowPortfolio = true,
                analyticsParams = FeedRoute.MarketsTokenDetails.AnalyticsParams(
                    blockchain = null,
                    source = AnalyticsParam.ScreensSources.Market.value,
                ),
            ),
        )
    }

    private fun CoroutineScope.loadQuotesWithTimer(timeMillis: Long) {
        launch {
            while (true) {
                delay(timeMillis)
                containerBottomSheetState.first { it == BottomSheetState.EXPANDED }
                isVisibleOnScreen.first { it }
                listManager.updateQuotes()
            }
        }.saveIn(updateQuotesJob)
    }
}