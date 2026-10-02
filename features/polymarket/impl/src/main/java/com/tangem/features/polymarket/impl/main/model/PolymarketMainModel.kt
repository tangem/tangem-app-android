package com.tangem.features.polymarket.impl.main.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketEventsBatchAction
import com.tangem.domain.polymarket.model.PolymarketEventsBatchingContext
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionBalancePoller
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketCredentialsPresenceUseCase
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.features.polymarket.impl.common.funding.PredictionFundingRequisites
import com.tangem.features.polymarket.impl.common.funding.RequisitesState
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketReceiveConfigFactory
import com.tangem.features.polymarket.impl.main.addfunds.dismissIfShown
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketCategoryTabUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PredictionBalanceUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PolymarketEventUMConverter
import com.tangem.features.polymarket.impl.main.model.transformer.PolymarketFeedContentTransformer
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketFeedScrollPosition
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionPositionsValueFetcher
import com.tangem.domain.polymarket.flow.PredictionPositionsValueSupplier
import com.tangem.domain.polymarket.model.PredictionPositionsValue
import com.tangem.domain.polymarket.usecase.CheckPolymarketGeoblockUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Model of the Discovery feed screen. */
@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class PolymarketMainModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getPolymarketEventsBatchFlowUseCase: GetPolymarketEventsBatchFlowUseCase,
    private val getPolymarketCategoriesUseCase: GetPolymarketCategoriesUseCase,
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val refreshPredictionDepositRequisitesInteractor: RefreshPredictionDepositRequisitesInteractor,
    private val checkPolymarketGeoblockUseCase: CheckPolymarketGeoblockUseCase,
    private val predictionAccountStatusFetcher: PredictionAccountStatusFetcher,
    private val predictionPositionsValueSupplier: PredictionPositionsValueSupplier,
    private val predictionPositionsValueFetcher: PredictionPositionsValueFetcher,
    private val predictionBalancePoller: PredictionBalancePoller,
    private val predictionFundingWindow: PredictionFundingWindow,
    private val observePolymarketCredentialsPresenceUseCase: ObservePolymarketCredentialsPresenceUseCase,
) : Model(), PolymarketAddFundsComponent.ModelCallback {

    private val params = paramsContainer.require<PolymarketMainParams>()

    private val balanceConverter = PredictionBalanceUMConverter(
        onAddFundsClick = ::onAddFundsClick,
        onWithdrawClick = ::onWithdrawClick,
        onBalanceModeClick = ::onBalanceModeClick,
    )

    private val activeStatus = MutableStateFlow<PredictionAccountStatusValue.Active?>(value = null)

    private val balanceMode = MutableStateFlow(PredictionBalanceMode.AVAILABLE)

    private val fundingRequisites = PredictionFundingRequisites(
        refreshInteractor = refreshPredictionDepositRequisitesInteractor,
        userWalletId = params.userWalletId,
        scope = modelScope,
    )

    private val tradingAllowance = PredictionTradingAllowance(
        checkPolymarketGeoblockUseCase = checkPolymarketGeoblockUseCase,
        dispatchers = dispatchers,
    )

    private val refreshJobHolder = JobHolder()

    val blocksRefresh = PolymarketMainBlocksRefresh()

    val fundingSheetNavigation = SlotNavigation<PolymarketFundingSheetConfig>()

    val uiState: StateFlow<PolymarketMainUM>
        field = MutableStateFlow(
            PolymarketMainUM(
                balance = balanceConverter.convert(
                    PredictionBalanceUMConverter.Input(
                        status = PredictionAccountStatusValue.Loading,
                        positions = PredictionPositionsValue.Unknown,
                        requisites = RequisitesState.Loading,
                        isTradingAllowed = false,
                        mode = PredictionBalanceMode.AVAILABLE,
                    ),
                ),
                categories = persistentListOf(),
                content = PolymarketMainUM.ContentUM.Loading,
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = { onRefresh() }),
            ),
        )

    private val eventUMConverter = PolymarketEventUMConverter(
        onEventClick = ::onEventClick,
        onOutcomeClick = ::onOutcomeClick,
    )

    private val categoryTabConverter = PolymarketCategoryTabUMConverter(onCategoryClick = ::onCategorySelected)

    private val staleDataReporter = PolymarketStaleDataReporter(onReport = ::reportStaleData)

    init {
        combine(
            flow = predictionAccountStatusSupplier(userWalletId = params.userWalletId),
            flow2 = predictionPositionsValueSupplier(userWalletId = params.userWalletId),
            flow3 = fundingRequisites.state,
            flow4 = tradingAllowance.isAllowed,
            flow5 = balanceMode,
            transform = { status, positions, requisites, isTradingAllowed, mode ->
                activeStatus.value = status as? PredictionAccountStatusValue.Active

                balanceConverter.convert(
                    PredictionBalanceUMConverter.Input(
                        status = status,
                        positions = positions,
                        requisites = requisites,
                        isTradingAllowed = isTradingAllowed,
                        mode = mode,
                    ),
                )
            },
        )
            .onEach { balance -> uiState.update { it.copy(balance = balance) } }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)

        predictionFundingWindow.isOpen(userWalletId = params.userWalletId)
            .onEach { isOpen -> uiState.update { it.copy(isAwaitingFunds = isOpen) } }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)

        predictionAccountStatusSupplier(userWalletId = params.userWalletId)
            .filterIsInstance<PredictionAccountStatusValue.Active>()
            .take(count = 1)
            .onEach { fundingRequisites.prefetchOnce() }
            .launchIn(modelScope)

        observePolymarketCredentialsPresenceUseCase(userWalletId = params.userWalletId)
            .filter { hasCredentials -> !hasCredentials }
            .take(count = 1)
            .onEach { returnToGate() }
            .launchIn(modelScope)
    }

    // Kept for as long as the screen lives: coming back to a tab must not cost a reload.
    private val feeds = mutableMapOf<Int?, PolymarketCategoryFeed>()

    private val activeFeed = MutableStateFlow<PolymarketCategoryFeed?>(value = null)

    private var categories: List<PolymarketCategory> = emptyList()
    private var selectedCategoryId: Int? = null
    private var isScreenResumed = false

    private val categoriesJob = JobHolder()

    private val balancePollJobHolder = JobHolder()

    init {
        observeEvents()
        loadCategories()
        modelScope.launch { tradingAllowance.refresh() }
    }

    fun onBackClick() {
        router.pop()
    }

    fun onSearchClick() {
        router.push(PolymarketRoute.Search(userWalletId = params.userWalletId))
    }

    fun onHistoryClick() {
        router.push(PolymarketRoute.History(userWalletId = params.userWalletId))
    }

    fun onLoadMore() {
        val feed = activeFeed.value ?: return

        modelScope.launch {
            when (val status = feed.batchFlow.state.value.status) {
                is PaginationStatus.Paginating -> {
                    val lastResult = status.lastResult
                    val canLoadMore = lastResult !is BatchFetchResult.Success || !lastResult.last
                    if (canLoadMore) feed.actionsFlow.emit(BatchAction.LoadMore())
                }
                else -> Unit
            }
        }
    }

    fun onVisibleEventsChange(eventIds: Set<String>) {
        activeFeed.value?.poller?.setVisibleEventIds(eventIds)
    }

    fun onScrollIdle(index: Int, offset: Int) {
        val feed = activeFeed.value ?: return

        feed.scrollPosition = PolymarketFeedScrollPosition(index = index, offset = offset)
        feed.poller.onScrollIdle()
    }

    fun onScrollPositionRestore() {
        uiState.update { it.copy(pendingScrollPosition = null) }
    }

    fun onResume() {
        isScreenResumed = true
        activeFeed.value?.poller?.resume()
        fundingRequisites.retryIfFailed()
        predictionBalancePoller.subscribe(userWalletId = params.userWalletId)
            .launchIn(modelScope)
            .saveIn(balancePollJobHolder)
    }

    private fun returnToGate() {
        TangemLogger.i("Main: no credentials to sign with, returning to the gate")
        router.replaceAll(PolymarketRoute.Onboarding(userWalletId = params.userWalletId))
    }

    fun onPause() {
        isScreenResumed = false
        activeFeed.value?.poller?.pause()
        balancePollJobHolder.cancel()
    }

    private fun loadCategories() {
        uiState.update { it.copy(isCategoriesLoading = true) }
        modelScope.launch {
            val result = withContext(dispatchers.default) { getPolymarketCategoriesUseCase() }
            val loadedCategories = result.getOrNull().orEmpty()

            categories = loadedCategories
            selectedCategoryId = loadedCategories.firstOrNull()?.id
            uiState.update { it.copy(categories = buildTabs(), isCategoriesLoading = false) }

            selectFeed(categoryId = selectedCategoryId, reloadVisited = true)
        }.saveIn(categoriesJob)
    }

    private fun onCategorySelected(categoryId: Int) {
        if (categoryId == selectedCategoryId) return
        selectedCategoryId = categoryId
        uiState.update { it.copy(categories = buildTabs()) }
        selectFeed(categoryId = categoryId)
    }

    private fun selectFeed(categoryId: Int?, reloadVisited: Boolean = false) {
        activeFeed.value?.poller?.pause()

        val visited = feeds[categoryId]
        val feed = visited ?: createFeed(config = PolymarketEventsListConfig(category = categoryId))
            .also { feeds[categoryId] = it }

        activeFeed.value = feed
        uiState.update { it.copy(pendingScrollPosition = feed.scrollPosition) }

        if (visited == null || reloadVisited) reloadEvents(feed = feed)
        if (isScreenResumed) feed.poller.resume()
    }

    private fun createFeed(config: PolymarketEventsListConfig): PolymarketCategoryFeed {
        // replay = 1: the first Reload is emitted before the pagination subscribes.
        val actionsFlow = MutableSharedFlow<PolymarketEventsBatchAction>(replay = 1)
        val batchFlow = getPolymarketEventsBatchFlowUseCase(
            context = PolymarketEventsBatchingContext(actionsFlow = actionsFlow, coroutineScope = modelScope),
        )
        val poller = PolymarketFeedPoller(
            scope = modelScope,
            batchFlow = batchFlow,
            actionsFlow = actionsFlow,
            onStaleData = staleDataReporter::report,
        )
        poller.start()

        return PolymarketCategoryFeed(
            config = config,
            actionsFlow = actionsFlow,
            batchFlow = batchFlow,
            poller = poller,
        )
    }

    private fun reload() {
        uiState.update { it.copy(content = PolymarketMainUM.ContentUM.Loading) }
        if (categories.isEmpty()) loadCategories() else activeFeed.value?.let(::reloadEvents)
    }

    private fun reloadEvents(feed: PolymarketCategoryFeed) {
        modelScope.launch {
            feed.poller.onFeedReloaded(feed.config)
            feed.actionsFlow.emit(BatchAction.Reload(requestParams = feed.config))
        }
    }

    private fun observeEvents() {
        activeFeed
            .filterNotNull()
            .flatMapLatest { feed -> feed.batchFlow.state }
            .onEach { batchState ->
                val transformer = PolymarketFeedContentTransformer(
                    batchListState = batchState,
                    eventUMConverter = eventUMConverter,
                    onReloadClick = ::reload,
                    onNextPageRetryClick = ::onLoadMore,
                )
                uiState.update(transformer::transform)
            }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun buildTabs() = categoryTabConverter.convert(
        value = PolymarketCategoryTabUMConverter.Input(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
        ),
    )

    private fun reportStaleData() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
    }

    private fun onEventClick(eventId: String) {
        router.push(
            PolymarketRoute.EventDetails(eventId = eventId, userWalletId = params.userWalletId),
        )
    }

    private fun onRefresh() {
        if (uiState.value.pullToRefreshConfig.isRefreshing) return
        setRefreshing(isRefreshing = true)

        modelScope.launch {
            runSuspendCatching {
                coroutineScope {
                    listOf(
                        async { tradingAllowance.refresh() },
                        async { blocksRefresh.refreshAll() },
                        async {
                            predictionAccountStatusFetcher(
                                PredictionAccountStatusFetcher.Params(userWalletId = params.userWalletId),
                            )
                        },
                        async {
                            predictionPositionsValueFetcher(
                                PredictionPositionsValueFetcher.Params(userWalletId = params.userWalletId),
                            )
                        },
                    ).awaitAll()
                }
                fundingRequisites.refresh()
            }.onFailure { error ->
                TangemLogger.e("Failed to refresh Polymarket main screen", error)
            }
        }.saveIn(refreshJobHolder).invokeOnCompletion {
            setRefreshing(isRefreshing = false)
        }
    }

    private fun setRefreshing(isRefreshing: Boolean) {
        uiState.update {
            it.copy(pullToRefreshConfig = it.pullToRefreshConfig.copy(isRefreshing = isRefreshing))
        }
    }

    private fun onBalanceModeClick() {
        balanceMode.update { it.toggled() }
    }

    private fun onAddFundsClick() {
        when (fundingRequisites.state.value) {
            is RequisitesState.Loading -> Unit
            is RequisitesState.Failed -> messageSender.send(
                DialogMessage(message = resourceReference(R.string.prediction_add_funds_error_description)),
            )
            is RequisitesState.Ready -> openAddFunds()
        }
    }

    private fun openAddFunds() {
        if (activeStatus.value?.cryptoCurrencyStatus == null) return reportUnconfirmedAddress()

        fundingSheetNavigation.activate(PolymarketFundingSheetConfig.AddFunds)
    }

    private fun onWithdrawClick() {
        val collateral = activeStatus.value?.cryptoCurrencyStatus?.currency ?: return reportUnconfirmedAddress()
        router.push(
            AppRoute.Swap(
                userWalletId = params.userWalletId,
                fromCryptoCurrency = collateral,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.FROM,
                screenSource = AnalyticsParam.ScreensSources.Predictions.value,
                accountFlow = AppRoute.Swap.AccountFlow.PredictionWithdraw,
            ),
        )
    }

    private fun reportUnconfirmedAddress() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_unknown_error)))
    }

    override fun onSwapClick() {
        val collateral = activeStatus.value?.cryptoCurrencyStatus?.currency ?: return
        fundingSheetNavigation.dismiss()
        router.push(
            AppRoute.Swap(
                userWalletId = params.userWalletId,
                fromCryptoCurrency = collateral,
                fromCurrencyPosition = AppRoute.Swap.CurrencyPosition.TO,
                screenSource = AnalyticsParam.ScreensSources.Predictions.value,
                accountFlow = AppRoute.Swap.AccountFlow.PredictionTopUp,
            ),
        )
    }

    override fun onReceiveClick() {
        val collateral = activeStatus.value?.cryptoCurrencyStatus?.currency ?: return
        val target = fundingRequisites.target ?: return
        fundingSheetNavigation.activate(
            PolymarketFundingSheetConfig.Receive(
                config = PolymarketReceiveConfigFactory.create(
                    cryptoCurrency = collateral,
                    userWalletId = params.userWalletId,
                    target = target,
                ),
            ),
        )
    }

    override fun onDismissAddFunds() {
        fundingSheetNavigation.dismissIfShown(PolymarketFundingSheetConfig.AddFunds)
    }

    private fun onOutcomeClick(eventId: String, marketId: String, assetId: String) {
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                userWalletId = params.userWalletId,
                marketId = marketId,
                assetId = assetId,
            ),
        )
    }
}