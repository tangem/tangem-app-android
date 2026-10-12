package com.tangem.features.feed.earn.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.domain.earn.usecase.*
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.earn.PreselectedEarnType
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.feed.earn.components.EarnBottomSheetRoute
import com.tangem.features.feed.earn.components.EarnNetworkFilterComponent
import com.tangem.features.feed.earn.components.EarnTypeFilterComponent
import com.tangem.features.feed.earn.model.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.earn.model.filters.state.EarnFilterChipsFactory
import com.tangem.features.feed.earn.model.portfolio.EarnAddToPortfolioLauncher
import com.tangem.features.feed.earn.model.state.EarnStateController
import com.tangem.features.feed.earn.model.state.transformers.*
import com.tangem.features.feed.earn.model.statemanager.EarnListBatchFlowManager
import com.tangem.features.feed.earn.model.statemanager.EarnListStateManager
import com.tangem.features.feed.earn.ui.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.earn.ui.state.EarnFeedTabUM
import com.tangem.features.feed.nav.FeedTabId
import com.tangem.features.feed.v2.FeedEntryController
import com.tangem.features.feed.v2.FeedEntryFilter
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class EarnFeedTabModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val fetchEarnNetworksUseCase: FetchEarnNetworksUseCase,
    private val getEarnNetworksUseCase: GetEarnNetworksUseCase,
    private val getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase,
    private val getTopEarnTokensUseCase: GetTopEarnTokensUseCase,
    private val fetchTopEarnTokensUseCase: FetchTopEarnTokensUseCase,
    private val getEarnFilterUseCase: GetEarnFilterUseCase,
    private val setEarnFilterUseCase: SetEarnFilterUseCase,
    private val appRouter: AppRouter,
    private val stateController: EarnStateController,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val feedEntryController: FeedEntryController,
) : Model() {

    private val earnNetworks = MutableStateFlow<EarnNetworks?>(null)

    /** `null` until the stored filter arrives; the chips shimmer meanwhile. */
    private val appliedFilter = MutableStateFlow<EarnFilter?>(null)

    private val currentFilter: EarnFilter get() = appliedFilter.value ?: DEFAULT_FILTER

    private val filterChipsFactory = EarnFilterChipsFactory(
        onNetworkClick = ::onNetworkFilterClick,
        onTypeClick = ::onTypeFilterClick,
        onNetworkClear = ::onClearNetworkFilterClick,
        onTypeClear = ::onClearTypeFilterClick,
    )

    private val earnListConfigProvider = Provider {
        createEarnTokensListConfig(filter = appliedFilter.value, earnNetworks = earnNetworks.value)
    }

    val bottomSheetNavigation: SlotNavigation<EarnBottomSheetRoute> = SlotNavigation()

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

    private val batchFlowManager = EarnListBatchFlowManager(
        getEarnTokensBatchFlowUseCase = getEarnTokensBatchFlowUseCase,
        configProvider = earnListConfigProvider,
        onItemClick = addToPortfolioLauncher::launch,
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    private var isOpenedReported = false

    val state: StateFlow<EarnFeedTabUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        fetchEarnNetworks()
        fetchTopEarnTokens()
        subscribeOnActiveFilters()
        subscribeOnNetworks()
        subscribeOnBatchFlow()
        subscribeToMostlyUsed()
        subscribeOnDeeplinkFilter()
    }

    private fun subscribeOnDeeplinkFilter() {
        modelScope.launch(dispatchers.default) {
            feedEntryController.pendingFilter
                .filterNotNull()
                .filter { it.tabId == FeedTabId.Earn }
                .collect(::applyDeeplinkFilter)
        }
    }

    /**
     * The stored filter has to arrive before the type is written: [currentFilter] falls back to
     * [DEFAULT_FILTER] until it does, so a deeplink landing during a cold start would reset the
     * network the user had chosen.
     */
    private suspend fun applyDeeplinkFilter(filter: FeedEntryFilter) {
        feedEntryController.consumeFilter()

        val type = filter.earnType ?: return
        val stored = appliedFilter.filterNotNull().first()

        setEarnFilterUseCase(stored.copy(earnFilterType = type.toEarnFilterType()))
    }

    private fun PreselectedEarnType.toEarnFilterType(): EarnFilterType = when (this) {
        PreselectedEarnType.Staking -> EarnFilterType.STAKING
        PreselectedEarnType.Yield -> EarnFilterType.YIELD
    }

    private fun subscribeOnBatchFlow() {
        combine(
            batchFlowManager.uiItems,
            batchFlowManager.initialLoadingError,
            batchFlowManager.paginationStatus,
        ) { items, error, paginationStatus ->
            val filter = currentFilter
            val hasActiveFilters = filter.earnFilterType != EarnFilterType.ALL ||
                filter.earnFilterNetwork !is EarnFilterNetwork.AllNetworks
            EarnListStateManager.calculateState(
                items = items,
                error = error,
                paginationStatus = paginationStatus,
                hasActiveFilters = hasActiveFilters,
                onRetryClick = {
                    batchFlowManager.reload()
                    reloadEarnNetworks()
                },
                onLoadMore = { batchFlowManager.loadMore() },
                onClearFiltersClick = ::onClearFiltersClick,
            ) to error
        }.onEach { (bestOpportunitiesState, error) ->
            error?.let(::handleBestOpportunitiesErrorAnalytics)
            stateController.update(UpdateBestOpportunitiesStateTransformer(bestOpportunitiesState))
        }.launchIn(modelScope)
    }

    private fun subscribeToMostlyUsed() {
        modelScope.launch(dispatchers.default) {
            getTopEarnTokensUseCase().collect { earnResult ->
                stateController.update(
                    UpdateOpportunitiesStateTransformer(
                        earnResult = earnResult,
                        onItemClick = addToPortfolioLauncher::launch,
                        onRetryClick = ::fetchTopEarnTokens,
                    ),
                )
            }
        }
    }

    private fun subscribeOnNetworks() {
        modelScope.launch(dispatchers.default) {
            getEarnNetworksUseCase().collect(earnNetworks)
        }
    }

    /**
     * The chips follow both the filter and the networks response, but the list query only follows the
     * networks when the filter scopes by them. Reloading on every emission therefore sent the first page
     * twice on a cold start — once with [earnNetworks] still null, once when it arrived — so the reload is
     * gated on the request config the manager will actually use.
     */
    private fun subscribeOnActiveFilters() {
        modelScope.launch(dispatchers.default) {
            combine(getEarnFilterUseCase(), earnNetworks) { filter, networks ->
                appliedFilter.value = filter
                stateController.update(
                    UpdateEarnFiltersTransformer(
                        filters = filterChipsFactory.create(filter = filter, networks = networks),
                    ),
                )
                earnListConfigProvider()
            }
                .distinctUntilChanged()
                .collect { batchFlowManager.reload() }
        }
    }

    private fun fetchTopEarnTokens() {
        modelScope.launch(dispatchers.default) {
            stateController.update(UpdateOpportunitiesStateLoadingTransformer())
            fetchTopEarnTokensUseCase()
        }
    }

    private fun fetchEarnNetworks() {
        modelScope.launch(dispatchers.default) {
            fetchEarnNetworksUseCase()
        }
    }

    private fun reloadEarnNetworks() {
        modelScope.launch(dispatchers.default) {
            if (earnNetworks.value?.isLeft() != false) {
                fetchEarnNetworks()
            }
        }
    }

    /* start of clicks area */
    private fun onTypeFilterClick() {
        bottomSheetNavigation.activate(
            EarnBottomSheetRoute.TypeFilter(
                params = EarnTypeFilterComponent.Params(
                    selectedFilter = currentFilter.earnFilterType,
                    onFilterSelected = ::onTypeFilterOptionSelected,
                    onDismiss = { bottomSheetNavigation.dismiss() },
                ),
            ),
        )
    }

    private fun onNetworkFilterClick() {
        bottomSheetNavigation.activate(
            EarnBottomSheetRoute.NetworkFilter(
                params = EarnNetworkFilterComponent.Params(
                    networks = earnNetworks.value?.getOrNull().orEmpty(),
                    selectedFilter = currentFilter.earnFilterNetwork,
                    onFilterSelected = ::onNetworkFilterOptionSelected,
                    onDismiss = { bottomSheetNavigation.dismiss() },
                ),
            ),
        )
    }

    private fun onClearFiltersClick() = applyFilter(DEFAULT_FILTER)

    private fun onClearNetworkFilterClick() {
        applyFilter(currentFilter.copy(earnFilterNetwork = DEFAULT_FILTER.earnFilterNetwork))
    }

    private fun onClearTypeFilterClick() {
        applyFilter(currentFilter.copy(earnFilterType = DEFAULT_FILTER.earnFilterType))
    }

    private fun onTypeFilterOptionSelected(type: EarnFilterType) {
        modelScope.launch(dispatchers.default) {
            setEarnFilterUseCase(currentFilter.copy(earnFilterType = type))
            bottomSheetNavigation.dismiss()
            reloadEarnNetworks()
        }
    }

    private fun onNetworkFilterOptionSelected(filter: EarnFilterNetwork) {
        applyFilter(currentFilter.copy(earnFilterNetwork = filter))
        bottomSheetNavigation.dismiss()
    }

    private fun applyFilter(filter: EarnFilter) {
        modelScope.launch(dispatchers.default) {
            setEarnFilterUseCase(filter)
        }
    }

    private fun onMostlyUsedScrolled() {
        analyticsEventHandler.send(EarnAnalyticsEvent.MostlyUsedCarouselScrolled())
    }
    /* end of clicks area */

    /** The tab page stays CREATED while unselected, so "Page Opened" is reported on first show, not on init. */
    fun onTabShown() {
        if (isOpenedReported) return
        isOpenedReported = true
        analyticsEventHandler.send(EarnAnalyticsEvent.EarnOpened())
    }

    private fun updateInitialState() {
        stateController.update(UpdateEarnFeedTabUMInitialStateTransformer(onScroll = ::onMostlyUsedScrolled))
    }

    private fun handleBestOpportunitiesErrorAnalytics(error: Throwable) {
        val (code, message) = when (error) {
            is ApiResponseError.HttpException -> error.code.numericCode to error.message.orEmpty()
            else -> null to ""
        }
        if (state.value.bestOpportunities !is EarnBestOpportunitiesUM.Error) {
            analyticsEventHandler.send(
                EarnAnalyticsEvent.BestOpportunitiesLoadError(
                    code = code,
                    message = message,
                ),
            )
        }
    }

    private companion object {

        val DEFAULT_FILTER = EarnFilter(
            earnFilterNetwork = EarnFilterNetwork.AllNetworks(isSelected = true),
            earnFilterType = EarnFilterType.ALL,
        )
    }
}