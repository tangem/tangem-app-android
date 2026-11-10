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
import com.tangem.domain.feedback.GetWalletMetaInfoUseCase
import com.tangem.domain.models.account.PredictionAccountStatusValue
import com.tangem.domain.polymarket.interactor.RefreshPredictionDepositRequisitesInteractor
import com.tangem.domain.polymarket.model.PolymarketCategory
import com.tangem.domain.polymarket.model.PolymarketEventsListConfig
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionBalancePoller
import com.tangem.domain.polymarket.flow.PredictionFundingWindow
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.domain.polymarket.usecase.ObservePolymarketLiveQuotesUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketSportsStateUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketCategoriesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketEventsBatchFlowUseCase
import com.tangem.domain.polymarket.usecase.ObservePolymarketCredentialsPresenceUseCase
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.features.polymarket.impl.common.funding.PredictionFundingRequisites
import com.tangem.features.polymarket.impl.common.funding.RequisitesState
import com.tangem.features.polymarket.impl.common.region.PredictionRegionAccess
import com.tangem.features.polymarket.impl.common.region.PredictionRegionStatus
import com.tangem.features.polymarket.impl.common.region.regionRestrictionsMessage
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketAddFundsComponent
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketFundingSheetConfig
import com.tangem.features.polymarket.impl.main.addfunds.PolymarketReceiveSheetFactory
import com.tangem.features.polymarket.impl.main.addfunds.dismissIfShown
import com.tangem.features.polymarket.impl.common.feed.model.converter.PolymarketCategoryTabUMConverter
import com.tangem.features.polymarket.impl.main.model.converter.PredictionBalanceUMConverter
import com.tangem.features.polymarket.impl.common.feed.model.converter.PolymarketEventUMConverter
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.features.polymarket.impl.main.ui.state.PolymarketMainUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.coroutines.saveIn
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
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
import com.tangem.features.polymarket.impl.common.feed.model.PolymarketFeedController
import com.tangem.features.polymarket.impl.common.feed.model.PolymarketStaleDataReporter
import com.tangem.features.polymarket.impl.common.feed.ui.state.PolymarketFeedContentUM

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
    private val regionAccess: PredictionRegionAccess,
    private val predictionAccountStatusFetcher: PredictionAccountStatusFetcher,
    private val predictionBalancePoller: PredictionBalancePoller,
    private val predictionFundingWindow: PredictionFundingWindow,
    private val observePolymarketCredentialsPresenceUseCase: ObservePolymarketCredentialsPresenceUseCase,
    private val getWalletMetaInfoUseCase: GetWalletMetaInfoUseCase,
    private val receiveSheetFactory: PolymarketReceiveSheetFactory,
    observePolymarketSportsStateUseCase: ObservePolymarketSportsStateUseCase,
    observePolymarketLiveQuotesUseCase: ObservePolymarketLiveQuotesUseCase,
    polymarketFeatureToggles: PolymarketFeatureToggles,
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

    private val refreshJobHolder = JobHolder()

    val blocksRefresh = PolymarketMainBlocksRefresh()

    val fundingSheetNavigation = SlotNavigation<PolymarketFundingSheetConfig>()

    val uiState: StateFlow<PolymarketMainUM>
        field = MutableStateFlow(
            PolymarketMainUM(
                balance = balanceConverter.convert(
                    PredictionBalanceUMConverter.Input(
                        status = PredictionAccountStatusValue.Loading,
                        requisites = RequisitesState.Loading,
                        regionStatus = PredictionRegionStatus.UNKNOWN,
                        mode = PredictionBalanceMode.AVAILABLE,
                    ),
                ),
                categories = persistentListOf(),
                content = PolymarketFeedContentUM.Loading,
                pullToRefreshConfig = PullToRefreshConfig(isRefreshing = false, onRefresh = { onRefresh() }),
                isFastTradingSettingsAvailable = polymarketFeatureToggles.isPolymarketSessionKeysEnabled,
            ),
        )

    private val isPhaseTwoEnabled = polymarketFeatureToggles.isPolymarketPhaseTwoEnabled

    private val eventUMConverter = PolymarketEventUMConverter(
        onEventClick = ::onEventClick,
        onOutcomeClick = ::onOutcomeClick,
        isPhaseTwoEnabled = polymarketFeatureToggles.isPolymarketPhaseTwoEnabled,
    )

    private val categoryTabConverter = PolymarketCategoryTabUMConverter(onCategoryClick = ::onCategorySelected)

    private val staleDataReporter = PolymarketStaleDataReporter(onReport = ::reportStaleData)

    init {
        combine(
            flow = predictionAccountStatusSupplier(userWalletId = params.userWalletId),
            flow2 = fundingRequisites.state,
            flow3 = regionAccess.status,
            flow4 = balanceMode,
            transform = { status, requisites, regionStatus, mode ->
                activeStatus.value = status as? PredictionAccountStatusValue.Active

                balanceConverter.convert(
                    PredictionBalanceUMConverter.Input(
                        status = status,
                        requisites = requisites,
                        regionStatus = regionStatus,
                        mode = mode,
                    ),
                )
            },
        )
            .onEach { balance -> uiState.update { it.copy(balance = balance) } }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)

        regionAccess.status
            .onEach { status ->
                uiState.update { it.copy(isRegionRestricted = status == PredictionRegionStatus.RESTRICTED) }
            }
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

    private var categories: List<PolymarketCategory> = emptyList()
    private var selectedCategoryId: Int? = null
    private val feedController = PolymarketFeedController(
        scope = modelScope,
        dispatchers = dispatchers,
        userWalletId = params.userWalletId,
        getEvents = getPolymarketEventsBatchFlowUseCase,
        observeSports = observePolymarketSportsStateUseCase,
        observeQuotes = observePolymarketLiveQuotesUseCase,
        isPhaseTwoEnabled = isPhaseTwoEnabled,
        eventConverter = eventUMConverter,
        onStaleData = staleDataReporter::report,
    )

    private val categoriesJob = JobHolder()

    private val balancePollJobHolder = JobHolder()

    init {
        feedController.content.onEach { content -> uiState.update { it.copy(content = content) } }.launchIn(modelScope)
        loadCategories()
        modelScope.launch { regionAccess.refresh() }
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

    fun onFastTradingSettingsClick() {
        router.push(PolymarketRoute.FastTradingSettings(userWalletId = params.userWalletId))
    }

    fun onSupportChatClick() {
        modelScope.launch {
            getWalletMetaInfoUseCase(params.userWalletId)
                .onRight { metaInfo ->
                    router.push(
                        AppRoute.Usedesk(
                            walletMetaInfo = metaInfo,
                            source = AnalyticsParam.ScreensSources.Predictions,
                        ),
                    )
                }
                .onLeft { TangemLogger.e("Failed to open the support chat from Predictions", it) }
        }
    }

    fun onLoadMore() = feedController.loadMore()

    fun onVisibleEventsChange(eventIds: Set<String>) = feedController.setVisibleEvents(eventIds)

    fun onScrollIdle(index: Int, offset: Int) = feedController.onScrollIdle(index, offset)

    fun onScrollPositionRestore() {
        uiState.update { it.copy(pendingScrollPosition = null) }
    }

    fun onResume() {
        feedController.resume()
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
        feedController.pause()
        balancePollJobHolder.cancel()
    }

    private fun loadCategories() {
        uiState.update { it.copy(isCategoriesLoading = true) }
        modelScope.launch {
            val result = withContext(
                dispatchers.default,
            ) { getPolymarketCategoriesUseCase(userWalletId = params.userWalletId) }
            val loadedCategories = result.getOrNull().orEmpty()

            categories = loadedCategories
            selectedCategoryId = loadedCategories.firstOrNull()?.id
            updateCategoryTabs()
            uiState.update { it.copy(isCategoriesLoading = false) }

            val categoryId = selectedCategoryId
            if (categoryId == null) {
                TangemLogger.e("Main: no category to request the feed for, categories answered $result")
                uiState.update { it.copy(content = PolymarketFeedContentUM.Error(onReloadClick = ::reload)) }
            } else {
                selectFeed(categoryId = categoryId, reloadVisited = true)
            }
        }.saveIn(categoriesJob)
    }

    private fun onCategorySelected(categoryId: Int) {
        if (categoryId == selectedCategoryId) return
        selectedCategoryId = categoryId
        updateCategoryTabs()
        selectFeed(categoryId = categoryId)
    }

    private fun updateCategoryTabs() {
        val category = selectedFeedCategory().takeIf { isPhaseTwoEnabled }
        uiState.update { state ->
            state.copy(
                categories = buildTabs(),
                onSeeAllClick = if (category?.subcategories.isNullOrEmpty()) null else ::openCategory,
            )
        }
    }

    private fun openCategory() {
        val category = selectedFeedCategory() ?: return
        router.push(PolymarketRoute.Category(userWalletId = params.userWalletId, category = category))
    }

    private fun selectFeed(categoryId: Int, reloadVisited: Boolean = false) {
        val selection = PolymarketEventsListConfig(category = categoryId)
        val scrollPosition = feedController.select(selection, reloadVisited)
        uiState.update { it.copy(selection = selection, pendingScrollPosition = scrollPosition) }
    }

    private fun reload() {
        uiState.update { it.copy(content = PolymarketFeedContentUM.Loading) }
        if (categories.isEmpty()) loadCategories() else feedController.reload()
    }

    private fun buildTabs() = categoryTabConverter.convert(
        value = PolymarketCategoryTabUMConverter.Input(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
        ),
    )

    private fun selectedFeedCategory(): PolymarketCategory? = categories.firstOrNull { it.id == selectedCategoryId }

    private fun reportStaleData() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.prediction_main_data_outdated)))
    }

    private fun onEventClick(eventId: String) {
        router.push(
            PolymarketRoute.EventDetails(
                eventId = eventId,
                userWalletId = params.userWalletId,
                categoryLabel = selectedFeedCategory()?.label,
            ),
        )
    }

    private fun onRefresh() {
        if (uiState.value.pullToRefreshConfig.isRefreshing) return
        setRefreshing(isRefreshing = true)

        modelScope.launch {
            runSuspendCatching {
                coroutineScope {
                    listOf(
                        async { regionAccess.refresh() },
                        async { blocksRefresh.refreshAll() },
                        async {
                            predictionAccountStatusFetcher(
                                PredictionAccountStatusFetcher.Params(userWalletId = params.userWalletId),
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
        if (regionAccess.isRestricted) return messageSender.send(regionRestrictionsMessage())

        whenRequisitesReady(::openAddFunds)
    }

    private fun whenRequisitesReady(action: () -> Unit) {
        when (fundingRequisites.state.value) {
            is RequisitesState.Loading -> Unit
            is RequisitesState.Failed -> messageSender.send(
                DialogMessage(
                    message = resourceReference(R.string.prediction_add_funds_error_description),
                    onDismissRequest = fundingRequisites::retryIfFailed,
                ),
            )
            is RequisitesState.Ready -> action()
        }
    }

    private fun openAddFunds() {
        if (activeStatus.value?.cryptoCurrencyStatus == null) return reportUnconfirmedAddress()

        fundingSheetNavigation.activate(PolymarketFundingSheetConfig.AddFunds)
    }

    private fun onWithdrawClick() {
        whenRequisitesReady(::openWithdraw)
    }

    private fun openWithdraw() {
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
        val requisites = fundingRequisites.ready ?: return
        fundingSheetNavigation.activate(
            receiveSheetFactory.create(
                collateral = collateral,
                userWalletId = params.userWalletId,
                requisites = requisites,
            ),
        )
    }

    override fun onDismissAddFunds() {
        fundingSheetNavigation.dismissIfShown(PolymarketFundingSheetConfig.AddFunds)
    }

    private fun onOutcomeClick(eventId: String, marketId: String, assetId: String) {
        if (regionAccess.isRestricted) return messageSender.send(regionRestrictionsMessage())

        router.push(
            PolymarketRoute.PlacePrediction(
                eventId = eventId,
                userWalletId = params.userWalletId,
                marketId = marketId,
                assetId = assetId,
            ),
        )
    }
}