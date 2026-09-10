package com.tangem.features.feed.earn.model.search

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.common.ui.markets.tokenselector.UserAssetEntrySectionsConverter
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.usecase.FetchEarnTokensCatalogUseCase
import com.tangem.domain.earn.usecase.GetEarnTokensCatalogUseCase
import com.tangem.domain.models.earn.EarnError
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.portfolio.UserAssetEntry
import com.tangem.domain.search.model.toEntries
import com.tangem.domain.search.usecase.GetSearchResultsUseCase
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.feed.earn.components.EarnBottomSheetRoute
import com.tangem.features.feed.earn.components.EarnFeedSearchTabComponent
import com.tangem.features.feed.earn.model.analytics.EarnSearchAnalyticsEvent
import com.tangem.features.feed.earn.model.analytics.EarnSource
import com.tangem.features.feed.earn.model.converter.EarnTokenWithCurrencyToTokenRowConverter
import com.tangem.features.feed.earn.model.portfolio.EarnAddToPortfolioLauncher
import com.tangem.features.feed.earn.ui.state.EarnSearchListUM
import com.tangem.features.feed.earn.ui.state.EarnSearchPortfolioUM
import com.tangem.features.feed.earn.ui.state.EarnSearchUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val RESULTS_SHOWN_DEBOUNCE_MS = 1_000L

@Suppress("LongParameterList")
@ModelScoped
internal class EarnFeedSearchTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getEarnTokensCatalogUseCase: GetEarnTokensCatalogUseCase,
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    userWalletsListRepository: UserWalletsListRepository,
    private val fetchEarnTokensCatalogUseCase: FetchEarnTokensCatalogUseCase,
    private val getSearchResultsUseCase: GetSearchResultsUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val appRouter: AppRouter,
    addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
) : Model() {

    private val query = paramsContainer.require<EarnFeedSearchTabComponent.Params>().query

    val containerBottomSheetState = MutableStateFlow(BottomSheetState.COLLAPSED)
    val isVisibleOnScreen = MutableStateFlow(false)

    val uiState: StateFlow<EarnSearchUM>
        field = MutableStateFlow(initialEarnSearchState())

    val bottomSheetNavigation: SlotNavigation<EarnBottomSheetRoute.AddToPortfolio> = SlotNavigation()

    private val addToPortfolioLauncher = EarnAddToPortfolioLauncher(
        addToPortfolioManagerFactory = addToPortfolioManagerFactory,
        analyticsEventHandler = analyticsEventHandler,
        appRouter = appRouter,
        modelScope = modelScope,
        showSheet = { source, manager ->
            bottomSheetNavigation.activate(
                EarnBottomSheetRoute.AddToPortfolio(source = source.value, manager = manager),
            )
        },
        dismissSheet = { bottomSheetNavigation.dismiss() },
    )

    private val rowConverter = EarnTokenWithCurrencyToTokenRowConverter(onItemClick = ::openEarnOpportunity)

    // one fetch at a time, and outside the per-query scope: the catalogue does not depend on the query,
    // so a keystroke must not cancel and restart it
    private val catalogLoadMutex = Mutex()

    /**
     * The trimmed query, published only once the tab is on screen. Results and the analytics counts hang
     * off this rather than off the raw query, so a tab the user never opened neither filters nor reports.
     */
    private val activeQuery = MutableStateFlow("")

    private val portfolioResultsCount = MutableStateFlow(0)
    private val earnResultsCount = MutableStateFlow<Int?>(null)

    private val catalog = getEarnTokensCatalogUseCase()
        .stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = null,
        )

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

        subscribeToEarnResults()
        subscribeToResultsShown()
    }

    private suspend fun onQueryChanged(query: String) {
        portfolioResultsCount.value = 0
        earnResultsCount.value = null
        activeQuery.value = query

        if (query.isEmpty()) {
            uiState.value = initialEarnSearchState()
            return
        }

        uiState.update { it.startingNewQuery(query) }

        loadCatalogIfNeverLoaded()
        coroutineScope {
            launch { observePortfolio(query) }
        }
    }

    private fun loadCatalogIfNeverLoaded() {
        if (catalog.value != null) return

        modelScope.launch {
            catalogLoadMutex.withLock {
                if (catalog.value != null) return@withLock
                fetchCatalog()
            }
        }
    }

    private fun retry() {
        uiState.update { it.copy(earn = EarnSearchListUM.Loading) }
        modelScope.launch {
            catalogLoadMutex.withLock { fetchCatalog() }
        }
    }

    private suspend fun fetchCatalog() {
        fetchEarnTokensCatalogUseCase()

        val error = getEarnTokensCatalogUseCase().first()?.leftOrNull() ?: return
        val (code, message) = when (error) {
            is EarnError.HttpError -> error.code to error.message
            is EarnError.NotHttpError -> null to ""
        }
        analyticsEventHandler.send(EarnSearchAnalyticsEvent.ErrorEarnData(code = code, message = message))
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
                EarnSearchPortfolioUM.Empty
            } else {
                EarnSearchPortfolioUM.Content(sections)
            }
            portfolioResultsCount.value = entriesCount
            uiState.update { state -> state.copy(portfolio = portfolio) }
        }
    }

    private fun subscribeToEarnResults() {
        combine(
            flow = activeQuery,
            flow2 = catalog,
        ) { currentQuery, catalogResult ->
            if (currentQuery.isEmpty()) return@combine null

            when (catalogResult) {
                null -> EarnSearchListUM.Loading
                else -> catalogResult.fold(
                    ifLeft = { EarnSearchListUM.Error(onRetryClicked = ::retry) },
                    ifRight = { items -> buildEarnContent(items = items, query = currentQuery) },
                )
            }
        }
            .filterNotNull()
            .onEach { snapshot ->
                earnResultsCount.value = when (snapshot) {
                    is EarnSearchListUM.Content -> snapshot.items.size
                    is EarnSearchListUM.NotFound -> 0
                    else -> null
                }
                uiState.update { it.copy(earn = snapshot) }
            }
            .launchIn(modelScope)
    }

    private suspend fun buildEarnContent(items: List<EarnTokenWithCurrency>, query: String): EarnSearchListUM {
        val rows = withContext(dispatchers.default) {
            items.matching(query).map(rowConverter::convert)
        }

        return if (rows.isEmpty()) {
            EarnSearchListUM.NotFound
        } else {
            EarnSearchListUM.Content(items = rows.toImmutableList())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun subscribeToResultsShown() {
        activeQuery
            .flatMapLatest { currentQuery ->
                if (currentQuery.isEmpty()) {
                    emptyFlow()
                } else {
                    combine(
                        flow = earnResultsCount.filterNotNull(),
                        flow2 = portfolioResultsCount,
                    ) { earnCount, portfolioCount -> earnCount to portfolioCount }
                        .debounce(RESULTS_SHOWN_DEBOUNCE_MS)
                        .take(1)
                }
            }
            .onEach { (earnCount, portfolioCount) ->
                analyticsEventHandler.send(
                    EarnSearchAnalyticsEvent.ResultsShown(
                        totalResultsCount = earnCount + portfolioCount,
                        earnResultsCount = earnCount,
                        userTokensResultsCount = portfolioCount,
                    ),
                )
            }
            .launchIn(modelScope)
    }

    private fun openEarnOpportunity(item: EarnTokenWithCurrency) {
        analyticsEventHandler.send(EarnSearchAnalyticsEvent.EarnItemClicked(item.earnToken.tokenSymbol))
        addToPortfolioLauncher.launch(earnTokenWithCurrency = item, source = EarnSource.SEARCH_SOURCE)
    }

    private fun openPortfolioToken(entry: UserAssetEntry) {
        analyticsEventHandler.send(
            EarnSearchAnalyticsEvent.PortfolioItemClicked(entry.currencyStatus.currency.symbol),
        )
        appRouter.push(
            AppRoute.CurrencyDetails(
                userWalletId = entry.userWalletId,
                currency = entry.currencyStatus.currency,
            ),
        )
    }
}