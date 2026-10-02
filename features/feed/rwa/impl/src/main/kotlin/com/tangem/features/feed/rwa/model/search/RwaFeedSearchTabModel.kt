package com.tangem.features.feed.rwa.model.search

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
import com.tangem.domain.feed.search.usecase.SaveFeedSearchQueryUseCase
import com.tangem.domain.markets.CoinCategory
import com.tangem.domain.markets.GetCoinCategoriesUseCase
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.search.model.toEntries
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.rwa.categories.CategorySelection
import com.tangem.features.feed.rwa.categories.ReservedCategoryCodes
import com.tangem.features.feed.rwa.model.UserCountryResolver
import com.tangem.features.feed.rwa.components.RwaFeedSearchTabComponent
import com.tangem.features.feed.rwa.model.analytics.RwaSearchAnalyticsEvent
import com.tangem.features.feed.rwa.model.list.RwaBatchFlowManager
import com.tangem.features.feed.rwa.model.list.RwaInterval
import com.tangem.features.feed.rwa.model.list.RwaListRequest
import com.tangem.features.feed.rwa.model.list.RwaSort
import com.tangem.features.feed.rwa.ui.state.RwaSearchListUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchPortfolioUM
import com.tangem.features.feed.rwa.ui.state.RwaSearchUM
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

private const val SEARCH_DEBOUNCE_MS = 500L
private const val UPDATE_QUOTES_TIMER_MILLIS = 60_000L
private const val RESULTS_SHOWN_DEBOUNCE_MS = 1_000L

/**
 * Model of the Real assets tab of the search screen: the user's matching holdings, then matching
 * real-world assets.
 */
@Suppress("LongParameterList", "TooManyFunctions")
@ModelScoped
internal class RwaFeedSearchTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    userWalletsListRepository: UserWalletsListRepository,
    private val getCoinCategoriesUseCase: GetCoinCategoriesUseCase,
    private val userCountryResolver: UserCountryResolver,
    private val getSearchResultsUseCase: GetSearchResultsUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val saveFeedSearchQueryUseCase: SaveFeedSearchQueryUseCase,
    private val router: Router,
) : Model() {

    private val query = paramsContainer.require<RwaFeedSearchTabComponent.Params>().query

    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    val uiState: StateFlow<RwaSearchUM>
        field = MutableStateFlow(initialRwaSearchState())

    private val updateQuotesJob = JobHolder()
    private val reloadJob = JobHolder()

    /** The `real-world-assets` scope every request is narrowed to; `null` until the fetch resolves it. */
    private val umbrella = MutableStateFlow<CategorySelection.Entry?>(null)
    private val isCategoriesFailed = MutableStateFlow(false)
    private var isLoadingCategories = false

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

    // search is fixed at the legacy scope: 24h trend, rating order, no sector. Only the umbrella and the
    // query change, and both are written before the reload that reads them
    private val searchRequest = MutableStateFlow(
        RwaListRequest(interval = RwaInterval.H24, sort = RwaSort.MarketCap, selection = CategorySelection()),
    )

    private val listManager = RwaBatchFlowManager(
        getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
        request = searchRequest,
        appCurrency = currentAppCurrency,
        onItemClick = ::openTokenDetails,
        modelScope = modelScope,
        dispatchers = dispatchers,
        batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
    )

    init {
        modelScope.launch {
            // the whole pager is built when the search screen opens, so no request may leave before the
            // user both selected this tab and typed something. The host renders the recents placeholder
            // instead of the pager while the query is blank, so being on screen implies a query exists
            isOnScreen.first { it }
            loadCategories()
            query
                .map(String::trim)
                .distinctUntilChanged()
                .collectLatest(::onQueryChanged)
        }

        subscribeToListItems()
        subscribeToAppCurrencyChanges()
        subscribeToResultsShown()
        subscribeToListLoadingErrors()

        listManager.onLastBatchLoadedSuccess.onEach { batchKey ->
            listManager.loadCharts(setOf(batchKey), RwaInterval.H24)
            modelScope.loadQuotesWithTimer(UPDATE_QUOTES_TIMER_MILLIS)
        }.launchIn(modelScope)
    }

    /**
     * Resolves the umbrella category, at most once in flight.
     *
     * Runs on [modelScope] rather than inside the query collector on purpose: `collectLatest` cancels its
     * scope on every keystroke, and a fetch started there would be restarted — and never finish — for a
     * user typing faster than it completes. The in-flight guard is what makes repeated calls free; a
     * cancel-and-restart would reintroduce the same starvation from the other side.
     */
    private fun loadCategories() {
        if (umbrella.value != null || isLoadingCategories) return

        isLoadingCategories = true
        isCategoriesFailed.value = false

        modelScope.launch {
            try {
                // the same country as the tab, so a result opened from either carries the same marks
                val country = userCountryResolver.resolve()
                searchRequest.update { it.copy(country = country) }

                getCoinCategoriesUseCase(country = country).fold(
                    ifLeft = { isCategoriesFailed.value = true },
                    ifRight = ::onCategoriesLoaded,
                )
            } finally {
                isLoadingCategories = false
            }
        }
    }

    private fun onCategoriesLoaded(categories: List<CoinCategory>) {
        val entry = categories
            .firstOrNull { it.code == ReservedCategoryCodes.REAL_WORLD_ASSETS }
            ?.let { CategorySelection.Entry(id = it.id, displayName = it.displayName, code = it.code) }

        if (entry == null) {
            isCategoriesFailed.value = true
        } else {
            umbrella.value = entry
        }
    }

    private suspend fun onQueryChanged(query: String) {
        portfolioResultsCount.value = 0

        if (query.isEmpty()) {
            listManager.clearStateAndStopAllActions()
            updateQuotesJob.cancel()
            uiState.value = initialRwaSearchState()
            return
        }

        uiState.update { it.startingNewQuery(query) }

        // collectLatest cancels this scope on the next keystroke, which both drops the pending reload and
        // stops the superseded portfolio subscription
        coroutineScope {
            launch { observePortfolio(query) }
            launch {
                delay(SEARCH_DEBOUNCE_MS)
                loadCategories()
                scheduleReload(query)
            }
        }
    }

    /**
     * The only place a search request is issued, so the umbrella can never be missing from one.
     *
     * Held in a [JobHolder] because the wait for the umbrella is open-ended: a fetch that failed leaves this
     * suspended until a retry resolves it, and without superseding, the retry's own request and the revived
     * original would both fire.
     */
    private fun scheduleReload(query: String) {
        modelScope.launch {
            val scope = umbrella.filterNotNull().first()

            searchRequest.value = searchRequest.value.copy(
                searchText = query,
                selection = CategorySelection(category = scope),
            )
            // the request flow is never collected, only read inside reload() — this call is what applies it
            listManager.reload()
        }.saveIn(reloadJob)
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
                RwaSearchPortfolioUM.Empty
            } else {
                RwaSearchPortfolioUM.Content(sections)
            }
            portfolioResultsCount.value = entriesCount
            uiState.update { state -> state.copy(portfolio = portfolio) }
        }
    }

    private fun subscribeToListItems() {
        combine(
            flow = query.map(String::trim).distinctUntilChanged(),
            flow2 = listManager.uiItems,
            flow3 = listManager.isEmptyResultState,
            flow4 = listManager.isInInitialLoadingErrorState,
            flow5 = isCategoriesFailed,
        ) { currentQuery, items, isEmptyResult, isInErrorState, isCategoriesFailed ->
            if (currentQuery.isEmpty()) return@combine null

            when {
                // a failed categories fetch outranks "not found": the query never reached the network
                isInErrorState || isCategoriesFailed -> RwaSearchListUM.Error(onRetry = ::retry)
                isEmptyResult -> RwaSearchListUM.NotFound
                items.isEmpty() -> RwaSearchListUM.Loading
                else -> RwaSearchListUM.Content(items = items, loadMore = listManager::loadMore)
            }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { snapshot -> uiState.update { it.applyListSnapshot(snapshot) } }
            .launchIn(modelScope)
    }

    private fun subscribeToAppCurrencyChanges() {
        modelScope.launch {
            // the same gate as the query collector: a change arriving while the user reads another tab
            // waits until this one is on screen instead of firing a request from behind it
            currentAppCurrency.drop(1).collectLatest { _ ->
                isOnScreen.first { it }
                val currentQuery = query.value.trim()
                if (currentQuery.isNotEmpty()) scheduleReload(currentQuery)
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
                    ) { listCount, portfolioCount -> listCount to portfolioCount }
                        .debounce(RESULTS_SHOWN_DEBOUNCE_MS)
                        .take(1)
                }
            }
            .onEach { (listCount, portfolioCount) ->
                analyticsEventHandler.send(
                    RwaSearchAnalyticsEvent.ResultsShown(
                        totalResultsCount = listCount + portfolioCount,
                        realAssetsResultsCount = listCount,
                        userTokensResultsCount = portfolioCount,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    private fun subscribeToListLoadingErrors() {
        listManager.initialLoadingError
            .onEach { throwable ->
                val code = (throwable as? ApiResponseError.HttpException)?.code?.numericCode
                analyticsEventHandler.send(
                    RwaSearchAnalyticsEvent.ErrorRealAssetsData(code = code, message = throwable.message.orEmpty()),
                )
            }
            .launchIn(modelScope)
    }

    /** Re-resolves the umbrella before retrying: a bare reload would request the whole coin universe. */
    private fun retry() {
        val currentQuery = query.value.trim()
        if (currentQuery.isEmpty()) return

        loadCategories()
        scheduleReload(currentQuery)
    }

    private fun openPortfolioToken(entry: UserAssetEntry) {
        analyticsEventHandler.send(
            RwaSearchAnalyticsEvent.PortfolioItemClicked(entry.currencyStatus.currency.symbol),
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
        analyticsEventHandler.send(RwaSearchAnalyticsEvent.RealAssetItemClicked(token.symbol))
        val params = token.toSerializableParam()

        // the query is saved for market results only: a portfolio asset is already on the user's
        // wallet screen, so offering the query that found it again is noise
        modelScope.launch { saveFeedSearchQueryUseCase(query.value) }

        router.push(
            route = FeedRoute.MarketsTokenDetails(
                token = params,
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