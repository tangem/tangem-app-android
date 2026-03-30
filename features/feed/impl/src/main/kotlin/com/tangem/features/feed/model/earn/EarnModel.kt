package com.tangem.features.feed.model.earn

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.domain.earn.usecase.*
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.feed.components.earn.DefaultEarnComponent
import com.tangem.features.feed.components.earn.EarnNetworkFilterComponent
import com.tangem.features.feed.components.earn.EarnTypeFilterComponent
import com.tangem.features.feed.components.feed.FeedBottomSheetRoute
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.model.earn.analytics.EarnAnalyticsEvent
import com.tangem.features.feed.model.earn.filters.state.EarnFilterNetworkConverter
import com.tangem.features.feed.model.earn.filters.state.EarnFilterNetworkUMConverter
import com.tangem.features.feed.model.earn.filters.state.EarnFilterTypeConverter
import com.tangem.features.feed.model.earn.filters.state.EarnFilterTypeUMConverter
import com.tangem.features.feed.model.earn.state.EarnStateController
import com.tangem.features.feed.model.earn.state.transformers.*
import com.tangem.features.feed.model.earn.statemanager.EarnListBatchFlowManager
import com.tangem.features.feed.model.earn.statemanager.EarnListStateManager
import com.tangem.features.feed.ui.earn.state.EarnBestOpportunitiesUM
import com.tangem.features.feed.ui.earn.state.EarnFilterNetworkUM
import com.tangem.features.feed.ui.earn.state.EarnFilterTypeUM
import com.tangem.features.feed.ui.earn.state.EarnUM
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class EarnModel @Inject constructor(
    paramsContainer: ParamsContainer,
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
) : Model() {

    private val params = paramsContainer.require<DefaultEarnComponent.Params>()
    private val earnNetworks = MutableStateFlow<EarnNetworks>(Either.Right(emptyList()))
    private val earnListConfigProvider = Provider {
        createEarnTokensListConfig(
            selectedTypeFilter = stateController.value.earnFilterUM.selectedTypeFilter,
            selectedNetworkFilter = stateController.value.earnFilterUM.selectedNetworkFilter,
            earnNetworks = earnNetworks.value,
        )
    }

    private val batchFlowManager = EarnListBatchFlowManager(
        getEarnTokensBatchFlowUseCase = getEarnTokensBatchFlowUseCase,
        configProvider = earnListConfigProvider,
        onItemClick = ::onEarnTokenClick,
        modelScope = modelScope,
        dispatchers = dispatchers,
    )

    val bottomSheetNavigation: SlotNavigation<FeedBottomSheetRoute> = SlotNavigation()

    val addToPortfolioCallback = object : AddToPortfolioPreselectedDataComponent.Callback {
        override fun onDismiss() = bottomSheetNavigation.dismiss()
        override fun onSuccess(addedToken: CryptoCurrency, walletId: UserWalletId) {
            bottomSheetNavigation.dismiss()
            appRouter.push(
                AppRoute.CurrencyDetails(
                    userWalletId = walletId,
                    currency = addedToken,
                ),
            )
        }
    }

    val state: StateFlow<EarnUM>
        get() = stateController.uiState

    init {
        updateInitialState()
        fetchEarnNetworks()
        subscribeOnStoredFilters()
        subscribeOnNetworks()
        subscribeOnBatchFlow()
        subscribeToMostlyUsed()
    }

    private fun subscribeOnBatchFlow() {
        combine(
            batchFlowManager.uiItems,
            batchFlowManager.initialLoadingError,
            batchFlowManager.paginationStatus,
        ) { items, error, paginationStatus ->
            val hasActiveFilters = state.value.earnFilterUM.selectedTypeFilter != EarnFilterTypeUM.All ||
                state.value.earnFilterUM.selectedNetworkFilter !is EarnFilterNetworkUM.AllNetworks
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
                    UpdateMostlyUsedStateTransformer(
                        earnResult = earnResult,
                        onItemClick = ::onEarnTokenClick,
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

    private fun subscribeOnStoredFilters() {
        modelScope.launch(dispatchers.default) {
            combine(
                getEarnFilterUseCase(),
                earnNetworks,
            ) { filter, networks ->
                val typeFilterUM = EarnFilterTypeConverter().convert(filter.earnFilterType)
                val networkFilterUM = EarnFilterNetworkConverter().convert(filter.earnFilterNetwork)
                stateController.update(
                    EarnFilterSelectedStateTransformer(
                        filterType = typeFilterUM,
                        filterNetwork = networkFilterUM,
                        earnNetworks = networks,
                    ),
                )
                batchFlowManager.reload()
            }.collect()
        }
    }

    private fun fetchTopEarnTokens() {
        modelScope.launch(dispatchers.default) {
            stateController.update(UpdateMostlyUsedStateLoadingTransformer())
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
            if (earnNetworks.value.isLeft()) {
                fetchEarnNetworks()
            }
        }
    }

    /* start of clicks area */
    private fun onTypeFilterClick() {
        val currentState = state.value
        bottomSheetNavigation.activate(
            FeedBottomSheetRoute.TypeFilter(
                params = EarnTypeFilterComponent.Params(
                    selectedFilter = EarnFilterTypeUMConverter().convert(currentState.earnFilterUM.selectedTypeFilter),
                    onFilterSelected = ::onTypeFilterOptionSelected,
                    onDismiss = { bottomSheetNavigation.dismiss() },
                ),
            ),
        )
    }

    private fun onNetworkFilterClick() {
        bottomSheetNavigation.activate(
            FeedBottomSheetRoute.NetworkFilter(
                params = EarnNetworkFilterComponent.Params(
                    allFilters = createNetworkFilters(),
                    onFilterSelected = ::onNetworkFilterOptionSelected,
                    onDismiss = { bottomSheetNavigation.dismiss() },
                ),
            ),
        )
    }

    private fun createNetworkFilters(): List<EarnFilterNetwork> {
        val selectedFilter = state.value.earnFilterUM.selectedNetworkFilter
        return buildList {
            add(
                EarnFilterNetwork.AllNetworks(
                    isSelected = selectedFilter is EarnFilterNetworkUM.AllNetworks,
                ),
            )
            add(
                EarnFilterNetwork.MyNetworks(
                    isSelected = selectedFilter is EarnFilterNetworkUM.MyNetworks,
                ),
            )
            earnNetworks.value.onRight { networks ->
                networks.mapTo(this) { network ->
                    EarnFilterNetwork.Specific(
                        id = network.networkId,
                        isSelected = selectedFilter is EarnFilterNetworkUM.Network &&
                            selectedFilter.id == network.networkId,
                        symbol = network.symbol,
                        fullName = network.fullName,
                    )
                }
            }
        }
    }

    private fun onClearFiltersClick() {
        modelScope.launch(dispatchers.default) {
            setEarnFilterUseCase(
                EarnFilter(
                    earnFilterNetwork = EarnFilterNetwork.AllNetworks(isSelected = true),
                    earnFilterType = EarnFilterType.ALL,
                ),
            )
        }
    }

    private fun onEarnTokenClick(earnTokenWithCurrency: EarnTokenWithCurrency, source: String) {
        analyticsEventHandler.send(
            EarnAnalyticsEvent.OpportunitySelected(
                tokenSymbol = earnTokenWithCurrency.earnToken.tokenSymbol,
                blockchain = earnTokenWithCurrency.cryptoCurrency.network.name,
                source = source,
            ),
        )
        bottomSheetNavigation.activate(
            FeedBottomSheetRoute.AddToPortfolio(
                tokenToAdd = AddToPortfolioPreselectedDataComponent.TokenToAdd(
                    network = TokenMarketInfo.Network(
                        networkId = earnTokenWithCurrency.earnToken.networkId,
                        isExchangeable = false,
                        contractAddress = earnTokenWithCurrency.earnToken.tokenAddress,
                        decimalCount = earnTokenWithCurrency.earnToken.decimalCount,
                    ),
                    id = CryptoCurrency.RawID(earnTokenWithCurrency.earnToken.tokenId),
                    name = earnTokenWithCurrency.earnToken.tokenName,
                    symbol = earnTokenWithCurrency.earnToken.tokenSymbol,
                ),
                source = source,
            ),
        )
    }

    private fun onTypeFilterOptionSelected(type: EarnFilterType) {
        modelScope.launch(dispatchers.default) {
            setEarnFilterUseCase(
                EarnFilter(
                    earnFilterNetwork = EarnFilterNetworkUMConverter().convert(
                        value = state.value.earnFilterUM.selectedNetworkFilter,
                    ),
                    earnFilterType = type,
                ),
            )
            bottomSheetNavigation.dismiss()
            reloadEarnNetworks()
        }
    }

    private fun onNetworkFilterOptionSelected(filter: EarnFilterNetwork) {
        modelScope.launch(dispatchers.default) {
            setEarnFilterUseCase(
                EarnFilter(
                    earnFilterNetwork = filter,
                    earnFilterType = EarnFilterTypeUMConverter().convert(state.value.earnFilterUM.selectedTypeFilter),
                ),
            )
        }
        bottomSheetNavigation.dismiss()
    }

    private fun onMostlyUsedScrolled() {
        analyticsEventHandler.send(EarnAnalyticsEvent.MostlyUsedCarouselScrolled())
    }
    /* end of clicks area */

    private fun updateInitialState() {
        analyticsEventHandler.send(EarnAnalyticsEvent.EarnOpened())
        stateController.update(
            UpdateEarnUMInitialStateTransformer(
                onBackClick = params.onBackClick,
                onNetworkFilterClick = ::onNetworkFilterClick,
                onTypeFilterClick = ::onTypeFilterClick,
                onScroll = ::onMostlyUsedScrolled,
                onSearchBarClicked = params.onSearchClicked,
            ),
        )
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
}