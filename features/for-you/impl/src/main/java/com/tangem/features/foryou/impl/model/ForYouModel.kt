package com.tangem.features.foryou.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.AppRouter
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.ds.tabs.TangemSegmentedPickerUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.earn.usecase.GetEarnTokensBatchFlowUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.*
import com.tangem.features.foryou.impl.model.converter.TOP_EARN_TOKENS_BATCH_SIZE
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.ForYouEarnOpportunitiesConverter
import com.tangem.features.foryou.impl.model.converter.portfolioReview.ForYouPortfolioReviewConverter
import com.tangem.features.foryou.impl.model.transformer.SetPortfolioReviewTransformer
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.combine6
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.job
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList")
internal class ForYouModel @Inject constructor(
    paramsContainer: ParamsContainer,
    userWalletsListRepository: UserWalletsListRepository,
    multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    private val router: AppRouter,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase,
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val earnErrorResolver: EarnErrorResolver,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
) : Model() {

    private val params = paramsContainer.require<ForYouComponent.Params>()

    private val expandedPortfolioReviewAssetIds = MutableStateFlow<Set<String>>(value = emptySet())
    private val expandedEarnOpportunitiesAssetIds = MutableStateFlow<Set<String>>(value = emptySet())
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    val bottomSheetNavigation: SlotNavigation<ForYouBottomSheetConfig> = SlotNavigation()

    var addToPortfolioManager: AddToPortfolioManager? = null
        private set

    private var addToPortfolioManagerScope: CoroutineScope? = null

    val uiState: StateFlow<ForYouUM>
        field = MutableStateFlow<ForYouUM>(
            ForYouUM(
                notifications = persistentListOf(),
                periodPickerUM = TangemSegmentedPickerUM(persistentListOf()),
                earnOpportunities = EarnOpportunitiesUM.Loading(
                    tokenList = buildList<ForYouTokenListItemUM> {
                        repeat(5) { index ->
                            add(
                                ForYouTokenListItemUM(
                                    tokenRowUM = TangemTokenRowUM.Loading(
                                        id = index.toString(),
                                    ),
                                    tokenList = persistentListOf(),
                                    isExpanded = false,
                                    isExpandable = false,
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
                onPeriodClick = ::onPeriodClick,
                portfolioReviewUM = PortfolioReviewUM.Loading(
                    marketChartUM = MarketChartUM.NoData(
                        title = resourceReference(R.string.market_chart_can_not_load_data),
                        donutText = resourceReference(R.string.market_chart_bubble_no_data),
                    ),
                    tokenList = buildList<ForYouTokenListItemUM> {
                        repeat(4) { index ->
                            add(
                                ForYouTokenListItemUM(
                                    tokenRowUM = TangemTokenRowUM.Loading(
                                        id = index.toString(),
                                    ),
                                    tokenList = persistentListOf(),
                                    isExpanded = false,
                                    isExpandable = false,
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
            ),
        )

    init {
        combine6(
            flow1 = userWalletsListRepository.selectedUserWallet,
            flow2 = multiAccountStatusListSupplier.invokeAsMap(),
            flow3 = expandedPortfolioReviewAssetIds,
            flow4 = expandedEarnOpportunitiesAssetIds,
            flow5 = yieldSupplyApyFlowUseCase(),
            flow6 = createTopEarnTokensFlow(),
        ) {
                globalSelectedWallet, accountList,
                expandedPortfolioReview, expandedEarnOpportunities,
                yieldAvailability, topEarnTokens,
            ->

            val stakingAvailability = accountList.flatMap { (userWalletId, accountStatusList) ->
                stakingAvailabilityListUseCase.invokeSync(
                    userWalletId = userWalletId,
                    cryptoCurrencyList = accountStatusList.flattenCurrencies().map { it.currency },
                ).entries
            }.associate { it.key to it.value }

            // TODO For You add choose portfolio flow
            val accountStatusList = accountList[globalSelectedWallet?.walletId]

            val portfolioReviewUM = ForYouPortfolioReviewConverter(
                appCurrency = selectedAppCurrencyFlow.value,
                expandedAssetIds = expandedPortfolioReview,
                expandClick = ::onExpandPortfolioReviewClick,
                onTokenClick = ::onPortfolioReviewTokenClick,
                onAddFundsClick = ::onAddFundsClick,
            ).convert(accountStatusList)

            val earnOpportunitiesUM = ForYouEarnOpportunitiesConverter(
                appCurrency = selectedAppCurrencyFlow.value,
                isAccountsModeEnabled = isAccountsModeEnabledUseCase.invokeSync(),
                yieldSupplyAvailability = yieldAvailability,
                yieldStakingAvailability = stakingAvailability,
                topEarnTokens = topEarnTokens,
                expandedAssetIds = expandedEarnOpportunities,
                expandClick = ::onExpandEarnOpportunitiesClick,
                onTokenClick = ::onEarnOpportunitiesTokenClick,
                onAllEarnTokensClick = params.callbacks::onAllEarnTokensClick,
            ).convert(accountStatusList)

            uiState.update(
                SetPortfolioReviewTransformer(
                    accountStatusList = accountStatusList,
                    portfolioReviewUM = portfolioReviewUM,
                    earnOpportunitiesUM = earnOpportunitiesUM,
                ),
            )
        }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    /**
     * Fetches the top-earn suggestions as a single batch of [TOP_EARN_TOKENS_BATCH_SIZE] tokens (a
     * one-shot [BatchAction.Reload]; no further paging). Emits `null` while the initial load is in
     * flight, a resolved error on failure, and the flattened token list on success — so the earn
     * section can distinguish "not loaded yet" from "loaded empty".
     */
    private fun createTopEarnTokensFlow(): Flow<EarnTopToken?> {
        val actionsFlow = MutableSharedFlow<BatchAction<Int, EarnTokensListConfig, Nothing>>(replay = 1)

        val batchFlow = getEarnTokensBatchFlowUseCase(
            context = EarnTokensBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = modelScope,
            ),
            batchSize = TOP_EARN_TOKENS_BATCH_SIZE,
        )

        actionsFlow.tryEmit(
            BatchAction.Reload(
                requestParams = EarnTokensListConfig(type = null, networks = null, isForEarn = false),
            ),
        )

        return batchFlow.state.map { state ->
            when (val status = state.status) {
                is PaginationStatus.None,
                is PaginationStatus.InitialLoading,
                -> null
                is PaginationStatus.InitialLoadingError -> earnErrorResolver.resolve(status.throwable).left()
                else -> state.data.flatMap { batch -> batch.data }.right()
            }
        }
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )
    }

    private fun onPortfolioReviewTokenClick(selectedWalletId: UserWalletId?, currency: CryptoCurrency) {
        val walletId = selectedWalletId ?: return
        params.callbacks.onTokenClick(walletId, currency)
    }

    private fun onEarnOpportunitiesTokenClick(
        selectedWalletId: UserWalletId?,
        currency: CryptoCurrency,
        type: ForYouEarnOpportunitiesType,
    ) {
        when {
            selectedWalletId != null -> openEarnScreen(
                userWalletId = selectedWalletId,
                currency = currency,
                type = type,
            )
            else -> {
                // TODO For you make logic if not added add token, otherwise manage funds
                // val token = RawMarketToken(
                //     id = currency.id.rawCurrencyId ?: return,
                //     name = currency.name,
                //     symbol = currency.symbol,
                // )
                // val network = TokenMarketInfo.Network(
                //     networkId = currency.network.rawId,
                //     isExchangeable = false,
                //     contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
                //     decimalCount = currency.decimals,
                // )
                // val manager = createAddToPortfolioManager().apply {
                //     setTokenParams(token)
                //     setTokenNetworks(listOf(network))
                // }
                // addToPortfolioManager = manager
                // Drop the slot through null so the same-source repeat click still recreates the child.
                bottomSheetNavigation.dismiss()
                bottomSheetNavigation.activate(
                    ForYouBottomSheetConfig.ManageFunds(
                        currency.id.rawCurrencyId ?: return,
                    ),
                )
            }
        }
    }

    private fun onExpandPortfolioReviewClick(assetId: String) {
        expandedPortfolioReviewAssetIds.update { ids ->
            if (assetId in ids) ids - assetId else ids + assetId
        }
    }

    private fun onExpandEarnOpportunitiesClick(assetId: String) {
        expandedEarnOpportunitiesAssetIds.update { ids ->
            if (assetId in ids) ids - assetId else ids + assetId
        }
    }

    private fun onPeriodClick(tangemSegmentUM: TangemSegmentUM) {
        uiState.update { state ->
            state.copy(
                periodPickerUM = state.periodPickerUM.copy(
                    initialSelectedItem = tangemSegmentUM,
                ),
            )
        }
    }

    // TODO For you make logic if not added add token, otherwise manage funds
    @Suppress("UnusedPrivateMember")
    private fun createAddToPortfolioManager(): AddToPortfolioManager {
        addToPortfolioManagerScope?.cancel()
        val managerScope = CoroutineScope(
            modelScope.coroutineContext + SupervisorJob(modelScope.coroutineContext.job),
        )
        addToPortfolioManagerScope = managerScope

        val manager = addToPortfolioManagerFactory.create(
            scope = managerScope,
            settings = AddToPortfolioManager.Settings.Earn,
            analyticsParams = AddToPortfolioManager.AnalyticsParams(
                source = AnalyticsParam.ScreensSources.Markets.value,
            ),
        ).apply {
            updateLaunchMode(AddToPortfolioManager.LaunchMode.ViaUserPortfolio)
        }

        manager.onDismiss.receiveAsFlow()
            .onEach { bottomSheetNavigation.dismiss() }
            .launchIn(managerScope)
        manager.onSuccessAdded.receiveAsFlow()
            .onEach { bottomSheetNavigation.dismiss() }
            .onEach { result ->
                router.push(
                    AppRoute.CurrencyDetails(
                        userWalletId = result.wallet.walletId,
                        currency = result.addedCurrency.currency,
                    ),
                )
            }
            .launchIn(managerScope)
        manager.onAddedTokenClick.receiveAsFlow()
            .onEach { bottomSheetNavigation.dismiss() }
            .onEach { result ->
                router.push(
                    AppRoute.CurrencyDetails(
                        userWalletId = result.wallet.walletId,
                        currency = result.addedCurrency.currency,
                    ),
                )
            }
            .launchIn(managerScope)

        return manager
    }

    private fun openEarnScreen(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        type: ForYouEarnOpportunitiesType,
    ) {
        router.push(
            when (type) {
                is ForYouEarnOpportunitiesType.Staking -> {
                    AppRoute.Staking(
                        userWalletId = userWalletId,
                        cryptoCurrency = currency,
                        integrationId = type.integrationID,
                    )
                }
                is ForYouEarnOpportunitiesType.YieldSupply -> {
                    AppRoute.YieldSupplyEntry(
                        userWalletId = userWalletId,
                        cryptoCurrency = currency,
                        apy = type.apy,
                    )
                }
            },
        )
    }

    private fun onAddFundsClick(userWalletId: UserWalletId) {
        bottomSheetNavigation.activate(ForYouBottomSheetConfig.AddFunds(userWalletId))
    }
}