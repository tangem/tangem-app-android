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
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.earn.EarnErrorResolver
import com.tangem.domain.earn.model.EarnTokensBatchingContext
import com.tangem.domain.earn.model.EarnTokensListConfig
import com.tangem.domain.earn.usecase.GetEarnTokensBatchFlowUseCase
import com.tangem.domain.markets.FetchCoinIndicatorsUseCase
import com.tangem.domain.markets.GetCoinIndicatorsUpdatesUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnTopToken
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.usecase.StakingAvailabilityListUseCase
import com.tangem.domain.yield.supply.usecase.YieldSupplyApyFlowUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioFetcher
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorController
import com.tangem.features.foryou.ForYouComponent
import com.tangem.features.foryou.impl.R
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.*
import com.tangem.features.foryou.impl.model.converter.ForYouPeriod
import com.tangem.features.foryou.impl.model.converter.TOP_EARN_TOKENS_BATCH_SIZE
import com.tangem.features.foryou.impl.model.converter.availableAccountIds
import com.tangem.features.foryou.impl.model.converter.earnOpportunities.ForYouEarnOpportunitiesConverter
import com.tangem.features.foryou.impl.model.converter.portfolioReview.ForYouPortfolioReviewConverter
import com.tangem.features.foryou.impl.model.converter.portfolioReview.ForYouSelectedPortfolioConverter
import com.tangem.features.foryou.impl.model.transformer.ApplyExpandedAssetsTransformer
import com.tangem.features.foryou.impl.model.transformer.ApplyExpandedAssetsTransformer.Section.EarnOpportunities
import com.tangem.features.foryou.impl.model.transformer.ApplyExpandedAssetsTransformer.Section.PortfolioReview
import com.tangem.features.foryou.impl.model.transformer.SetPortfolioReviewTransformer
import com.tangem.pagination.BatchAction
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.transformer.Transformer
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Stable
@ModelScoped
@Suppress("LongParameterList", "LargeClass")
internal class ForYouModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    yieldSupplyApyFlowUseCase: YieldSupplyApyFlowUseCase,
    getCoinIndicatorsUpdatesUseCase: GetCoinIndicatorsUpdatesUseCase,
    private val router: AppRouter,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val fetchCoinIndicatorsUseCase: FetchCoinIndicatorsUseCase,
    private val getEarnTokensBatchFlowUseCase: GetEarnTokensBatchFlowUseCase,
    private val stakingAvailabilityListUseCase: StakingAvailabilityListUseCase,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val earnErrorResolver: EarnErrorResolver,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val portfolioFetcherFactory: PortfolioFetcher.Factory,
    val portfolioSelectorController: PortfolioSelectorController,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    private val params = paramsContainer.require<ForYouComponent.Params>()

    val bottomSheetNavigation: SlotNavigation<ForYouBottomSheetConfig> = SlotNavigation()

    val portfolioFetcher: PortfolioFetcher by lazy {
        portfolioFetcherFactory.create(
            mode = PortfolioFetcher.Mode.All(isOnlyMultiCurrency = false),
            scope = modelScope,
        )
    }

    val portfolioSelectorCallback = object : PortfolioSelectorComponent.BottomSheetCallback {
        override val onDismiss: () -> Unit = { bottomSheetNavigation.dismiss() }
        override val onBack: () -> Unit = { bottomSheetNavigation.dismiss() }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val selectedPortfolio: Flow<ForYouSelectedPortfolio> =
        portfolioSelectorController
            .selectedAccounts
            .onEach { bottomSheetNavigation.dismiss() }
            .distinctUntilChanged()
            .flatMapLatest { selectedAccounts ->
                val converter = ForYouSelectedPortfolioConverter(selectedAccounts)
                multiAccountStatusListSupplier.invokeAsMap().map(converter::convert)
            }

    private val expandedPortfolioReviewAssetIds = MutableStateFlow<Set<String>>(value = emptySet())
    private val expandedEarnOpportunitiesAssetIds = MutableStateFlow<Set<String>>(value = emptySet())
    private val selectedPeriod = MutableStateFlow(value = ForYouPeriod.Day)
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

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
                portfolioSelectorLabel = stringReference("All account"),
                onSelectPortfolioClick = ::onSelectPortfolioClick,
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
        initDefaultPortfolioSelection()
        createCoinIndicatorsFetchFlow().launchIn(modelScope)

        // Expand/collapse clicks bypass the conversion pipeline below: they only patch isExpanded on the
        // affected item in the current state, so unaffected rows keep their instances and skip recomposition.
        expandedPortfolioReviewAssetIds.updateStateOnEach { ApplyExpandedAssetsTransformer(it, PortfolioReview) }
        expandedEarnOpportunitiesAssetIds.updateStateOnEach { ApplyExpandedAssetsTransformer(it, EarnOpportunities) }

        combine(
            flow = selectedPortfolio,
            flow2 = yieldSupplyApyFlowUseCase(),
            flow3 = createTopEarnTokensFlow(),
            flow4 = combine(
                getCoinIndicatorsUpdatesUseCase(),
                selectedPeriod,
                getBalanceHidingSettingsUseCase.isBalanceHidden(),
            ) { indicators, period, isBalanceHidden ->
                Triple(indicators, period, isBalanceHidden)
            },
            flow5 = userWalletsListRepository.selectedUserWallet,
        ) { selectedPortfolio, yieldAvailability, topEarnTokens, indicatorsPeriodHidden, selectedUserWallet ->
            val (indicators, period, isBalanceHidden) = indicatorsPeriodHidden

            val stakingAvailability = selectedPortfolio.accountCryptoCurrencyStatuses
                .groupBy { it.account.userWalletId }
                .flatMap { (userWalletId, statuses) ->
                    stakingAvailabilityListUseCase.invokeSync(
                        userWalletId = userWalletId,
                        cryptoCurrencyList = statuses.map { it.status.currency },
                    ).entries
                }
                .associate { it.key to it.value }

            val portfolioReviewUM = ForYouPortfolioReviewConverter(
                appCurrency = selectedAppCurrencyFlow.value,
                expandClick = ::onExpandPortfolioReviewClick,
                onTokenClick = ::onPortfolioReviewTokenClick,
                onAddFundsClick = ::onAddFundsClick,
                selectedWalletId = selectedUserWallet?.walletId,
                coinIndicators = indicators,
                timeframe = period.timeframe,
                isBalanceHidden = isBalanceHidden,
            ).convert(selectedPortfolio)

            val earnOpportunitiesUM = ForYouEarnOpportunitiesConverter(
                appCurrency = selectedAppCurrencyFlow.value,
                isAccountsModeEnabled = isAccountsModeEnabledUseCase.invokeSync(),
                yieldSupplyAvailability = yieldAvailability,
                yieldStakingAvailability = stakingAvailability,
                topEarnTokens = topEarnTokens,
                expandClick = ::onExpandEarnOpportunitiesClick,
                onTokenClick = ::onEarnOpportunitiesTokenClick,
                onAllEarnTokensClick = params.callbacks::onAllEarnTokensClick,
                isBalanceHidden = isBalanceHidden,
            ).convert(selectedPortfolio)

            uiState.update(
                SetPortfolioReviewTransformer(
                    selectedPortfolio = selectedPortfolio,
                    portfolioReviewUM = portfolioReviewUM,
                    earnOpportunitiesUM = earnOpportunitiesUM,
                    expandedPortfolioReviewAssetIds = expandedPortfolioReviewAssetIds::value,
                    expandedEarnOpportunitiesAssetIds = expandedEarnOpportunitiesAssetIds::value,
                ),
            )
        }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun initDefaultPortfolioSelection() {
        modelScope.launch {
            val accountList = multiAccountStatusListSupplier.invokeAsMap()
                .firstOrNull { it.availableAccountIds().isNotEmpty() }
                ?: return@launch

            if (portfolioSelectorController.selectedAccountsSync.isEmpty()) {
                portfolioSelectorController.selectAccount(accountList.availableAccountIds())
            }
        }
    }

    private fun onSelectPortfolioClick() {
        bottomSheetNavigation.activate(ForYouBottomSheetConfig.PortfolioSelector)
    }

    /**
     * Triggers a coin-indicators fetch for all distinct symbols of the selected wallet's portfolio.
     * The results land in the session store observed via [GetCoinIndicatorsUpdatesUseCase], so
     * already-stored readings render instantly while the fetch refreshes them in the background.
     * The symbol set is normalized (uppercase, distinct, sorted) and deduplicated so balance-driven
     * reorders don't refetch, while wallet switches and portfolio membership changes do. A failed
     * fetch keeps whatever readings the session store already holds.
     */
    private fun createCoinIndicatorsFetchFlow(): Flow<List<String>> {
        return combine(
            userWalletsListRepository.selectedUserWallet,
            multiAccountStatusListSupplier.invokeAsMap(),
        ) { globalSelectedWallet, accountList ->
            accountList[globalSelectedWallet?.walletId]?.flattenCurrencies().orEmpty()
                .map { it.currency.symbol.uppercase() }
                .distinct()
                .sorted()
        }
            .distinctUntilChanged()
            .onEach { symbols ->
                if (symbols.isNotEmpty()) {
                    fetchCoinIndicatorsUseCase(symbols = symbols)
                }
            }
            .flowOn(dispatchers.default)
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

    private fun <T> StateFlow<T>.updateStateOnEach(transformer: (T) -> Transformer<ForYouUM>) {
        onEach { uiState.update(transformer(it)) }.launchIn(modelScope)
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
        if (tangemSegmentUM.id == selectedPeriod.value.id) return

        uiState.update { state ->
            state.copy(
                periodPickerUM = state.periodPickerUM.copy(
                    initialSelectedItem = tangemSegmentUM,
                ),
            )
        }
        selectedPeriod.value = ForYouPeriod.fromId(tangemSegmentUM.id)
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