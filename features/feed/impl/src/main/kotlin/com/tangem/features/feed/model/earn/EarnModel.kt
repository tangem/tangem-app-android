package com.tangem.features.feed.model.earn

import androidx.compose.runtime.Stable
import arrow.core.Either
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.R
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.earn.usecase.*
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnNetworks
import com.tangem.domain.models.earn.EarnTokenWithCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.feed.components.earn.DefaultEarnComponent
import com.tangem.features.feed.components.feed.FeedPortfolioRoute
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioPreselectedDataComponent
import com.tangem.features.feed.model.earn.state.EarnStateController
import com.tangem.features.feed.model.earn.state.converter.EarnNetworksConverter
import com.tangem.features.feed.model.earn.state.transformers.*
import com.tangem.features.feed.model.earn.statemanager.EarnListBatchFlowManager
import com.tangem.features.feed.model.earn.statemanager.EarnListStateManager
import com.tangem.features.feed.ui.earn.state.*
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
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
    private val appRouter: AppRouter,
    private val stateController: EarnStateController,
) : Model() {

    private val params = paramsContainer.require<DefaultEarnComponent.Params>()
    private val earnNetworks = MutableStateFlow<EarnNetworks>(Either.Right(emptyList()))
    private val earnNetworksConverter: EarnNetworksConverter by lazy { EarnNetworksConverter() }
    private val earnListConfigProvider = Provider {
        createEarnTokensListConfig(
            selectedTypeFilter = stateController.value.selectedTypeFilter,
            selectedNetworkFilter = stateController.value.selectedNetworkFilter,
            earnNetworks = earnNetworks.value,
        )
    }

    private val batchFlowManager by lazy {
        EarnListBatchFlowManager(
            getEarnTokensBatchFlowUseCase = getEarnTokensBatchFlowUseCase,
            configProvider = earnListConfigProvider,
            onItemClick = ::onEarnTokenClick,
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    val bottomSheetNavigation: SlotNavigation<FeedPortfolioRoute> = SlotNavigation()

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
        fetchTopEarnTokens()
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
            val hasActiveFilters = state.value.selectedTypeFilter != EarnFilterTypeUM.All ||
                state.value.selectedNetworkFilter !is EarnFilterNetworkUM.AllNetworks
            EarnListStateManager.calculateState(
                items = items,
                error = error,
                paginationStatus = paginationStatus,
                hasActiveFilters = hasActiveFilters,
                onRetryClick = { batchFlowManager.reload() },
                onLoadMore = { batchFlowManager.loadMore() },
                onClearFiltersClick = ::onClearFiltersClick,
            )
        }.onEach { bestOpportunitiesState ->
            stateController.update(UpdateBestOpportunitiesStateTransformer(bestOpportunitiesState))
        }
            .onStart { batchFlowManager.reload() }
            .launchIn(modelScope)
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

    private fun fetchTopEarnTokens() {
        modelScope.launch(dispatchers.default) {
            fetchTopEarnTokensUseCase()
        }
    }

    private fun fetchEarnNetworks() {
        modelScope.launch(dispatchers.default) {
            fetchEarnNetworksUseCase()
        }
    }

    private fun createTypeFilterBottomSheetConfig(
        selectedOption: EarnFilterTypeUM,
        isShown: Boolean = false,
    ): TangemBottomSheetConfig {
        return TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = ::hideTypeFilterBottomSheet,
            content = EarnFilterByTypeBottomSheetContentUM(
                selectedOption = selectedOption,
                onOptionClick = ::onTypeFilterOptionSelected,
            ),
        )
    }

    private fun createNetworkFilterBottomSheetConfig(
        selectedNetwork: EarnFilterNetworkUM,
        isShown: Boolean = false,
    ): TangemBottomSheetConfig {
        val filters = buildList {
            add(
                EarnFilterNetworkUM.AllNetworks(
                    text = TextReference.Res(R.string.earn_filter_all_networks),
                    isSelected = false,
                ),
            )
            add(
                EarnFilterNetworkUM.MyNetworks(
                    text = TextReference.Res(R.string.earn_filter_my_networks),
                    isSelected = false,
                ),
            )
            earnNetworks.value.onRight { networks ->
                addAll(networks.map(earnNetworksConverter::convert))
            }
        }

        val updatedNetworks = filters.map { filter ->
            val shouldBeSelected = when (filter) {
                is EarnFilterNetworkUM.AllNetworks -> selectedNetwork is EarnFilterNetworkUM.AllNetworks
                is EarnFilterNetworkUM.MyNetworks -> selectedNetwork is EarnFilterNetworkUM.MyNetworks
                is EarnFilterNetworkUM.Network ->
                    selectedNetwork is EarnFilterNetworkUM.Network && filter.id == selectedNetwork.id
            }

            if (filter.isSelected != shouldBeSelected) {
                when (filter) {
                    is EarnFilterNetworkUM.AllNetworks -> filter.copy(isSelected = shouldBeSelected)
                    is EarnFilterNetworkUM.MyNetworks -> filter.copy(isSelected = shouldBeSelected)
                    is EarnFilterNetworkUM.Network -> filter.copy(isSelected = shouldBeSelected)
                }
            } else {
                filter
            }
        }

        return TangemBottomSheetConfig(
            isShown = isShown,
            onDismissRequest = ::hideNetworkFilterBottomSheet,
            content = EarnFilterByNetworkBottomSheetContentUM(
                selectedNetwork = selectedNetwork,
                networks = persistentListOf(*updatedNetworks.toTypedArray()),
                onOptionClick = ::onNetworkFilterOptionSelected,
            ),
        )
    }

    /* start of clicks area */
    private fun onTypeFilterClick() {
        stateController.update(
            EarnTypeFilterStateTransformer(
                shouldShow = true,
                filterByTypeBottomSheetConfig = ::createTypeFilterBottomSheetConfig,
            ),
        )
    }

    private fun hideTypeFilterBottomSheet() {
        stateController.update(
            EarnTypeFilterStateTransformer(
                shouldShow = false,
                filterByTypeBottomSheetConfig = ::createTypeFilterBottomSheetConfig,
            ),
        )
    }

    private fun onNetworkFilterClick() {
        stateController.update(
            EarnNetworkFilterStateTransformer(
                shouldShow = true,
                filterByNetworkBottomSheetConfig = ::createNetworkFilterBottomSheetConfig,
            ),
        )
    }

    private fun hideNetworkFilterBottomSheet() {
        stateController.update(
            EarnNetworkFilterStateTransformer(
                shouldShow = false,
                filterByNetworkBottomSheetConfig = ::createNetworkFilterBottomSheetConfig,
            ),
        )
    }

    private fun onTypeFilterOptionSelected(type: EarnFilterTypeUM) {
        stateController.update(
            EarnTypeFilterSelectedStateTransformer(
                filter = type,
                filterByTypeBottomSheetConfig = ::createTypeFilterBottomSheetConfig,
            ),
        )
        batchFlowManager.reload()
    }

    private fun onNetworkFilterOptionSelected(network: EarnFilterNetworkUM) {
        stateController.update(
            EarnNetworkFilterSelectedStateTransformer(
                filter = network,
                filterByNetworkBottomSheetConfig = ::createNetworkFilterBottomSheetConfig,
            ),
        )
        batchFlowManager.reload()
    }

    private fun onClearFiltersClick() {
        stateController.update(
            ClearEarnUMFilterStateTransformer(
                filterByTypeBottomSheetConfig = ::createTypeFilterBottomSheetConfig,
                filterByNetworkBottomSheetConfig = ::createNetworkFilterBottomSheetConfig,
            ),
        )
        batchFlowManager.reload()
    }

    private fun onEarnTokenClick(earnTokenWithCurrency: EarnTokenWithCurrency) {
        bottomSheetNavigation.activate(
            FeedPortfolioRoute.AddToPortfolio(
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
            ),
        )
    }
    /* end of clicks area */

    private fun updateInitialState() {
        stateController.update(
            UpdateEarnUMInitialStateTransformer(
                onBackClick = params.onBackClick,
                onNetworkFilterClick = ::onNetworkFilterClick,
                filterByTypeBottomSheetConfig = createTypeFilterBottomSheetConfig(state.value.selectedTypeFilter),
                filterByNetworkBottomSheetConfig = createNetworkFilterBottomSheetConfig(
                    selectedNetwork = state.value.selectedNetworkFilter,
                ),
                onTypeFilterClick = ::onTypeFilterClick,
            ),
        )
    }
}