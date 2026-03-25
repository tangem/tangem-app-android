package com.tangem.features.onramp.swap.availablepairs.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.ui.markets.models.MarketsListItemUM
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam.ScreensSources
import com.tangem.core.analytics.models.event.SwapAnalyticsEvent
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.GetAccountCurrencyStatusUseCase
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.card.common.extensions.hotWalletExcludedBlockchains
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.markets.GetMarketsTokenListFlowUseCase
import com.tangem.domain.markets.TokenMarketInfo
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.domain.markets.toSerializableParam
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.swap.domain.GetAvailablePairsUseCase
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioComponent
import com.tangem.features.feed.components.market.details.portfolio.add.AddToPortfolioManager
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.availablepairs.AvailableSwapPairsComponent
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetErrorWarningTransformer
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetNoAvailablePairsTransformer
import com.tangem.features.onramp.swap.availablepairs.market.SwapMarketsListBatchFlowManager
import com.tangem.features.onramp.swap.availablepairs.market.state.SwapMarketState
import com.tangem.features.onramp.swap.entity.AccountAvailabilityUM
import com.tangem.features.onramp.swap.entity.AccountCurrencyUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMController
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.SetLoadingAccountTokenListTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.SetNothingToFoundStateTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.UpdateAccountTokenListTransformer
import com.tangem.features.onramp.tokenlist.entity.utils.OnrampTokenItemStateConverterFactory
import com.tangem.features.onramp.utils.ClearSearchBarTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarCallbacksTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.features.swap.SwapFeatureToggles
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.ui.R as CoreUiR

private typealias AvailablePairsState = Lce<Throwable, List<SwapPairLeast>>

@Suppress("LongParameterList", "LargeClass")
internal class AvailableSwapPairsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val tokenListUMController: TokenListUMController,
    private val searchManager: InputManager,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getAvailablePairsUseCase: GetAvailablePairsUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getMarketsTokenListFlowUseCase: GetMarketsTokenListFlowUseCase,
    private val addToPortfolioManagerFactory: AddToPortfolioManager.Factory,
    private val excludedBlockchains: ExcludedBlockchains,
    private val getAccountCurrencyStatusUseCase: GetAccountCurrencyStatusUseCase,
    swapFeatureToggles: SwapFeatureToggles,
    getWalletsUseCase: GetWalletsUseCase,
) : Model() {

    val state: StateFlow<TokenListUM> = tokenListUMController.state

    private val params: AvailableSwapPairsComponent.Params = paramsContainer.require()
    private val userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }
    private val allUserWallets = getWalletsUseCase.invokeSync()

    val bottomSheetNavigation: SlotNavigation<AddToPortfolioRoute> = SlotNavigation()
    var addToPortfolioManager: AddToPortfolioManager? = null
    val addToPortfolioCallback: AddToPortfolioComponent.Callback = object : AddToPortfolioComponent.Callback {
        override fun onDismiss() = bottomSheetNavigation.dismiss()
        override fun onSuccess(addedToken: CryptoCurrency) {
            onTokenAddedToPortfolio(addedToken)
        }
    }
    private val addToPortfolioJobHolder = JobHolder()

    private val accountListFlow = getAccountListUseCaseFlow()
    private val availablePairsByNetworkFlow = MutableStateFlow<Map<LeastTokenInfo, AvailablePairsState>>(emptyMap())

    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = getSelectedAppCurrencyUseCase.invokeOrDefault()
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = AppCurrency.Default)
    private val refreshPairsTrigger = MutableSharedFlow<Unit>()
    private val searchQueryStateForMarkets = MutableStateFlow("")
    private val visibleMarketItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    private val defaultMarketsListManager by lazy {
        SwapMarketsListBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Main,
            order = TokenMarketListConfig.Order.Trending,
            currentAppCurrency = Provider { selectedAppCurrencyFlow.value },
            currentSearchText = Provider { null },
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    private val searchMarketsListManager by lazy {
        SwapMarketsListBatchFlowManager(
            getMarketsTokenListFlowUseCase = getMarketsTokenListFlowUseCase,
            batchFlowType = GetMarketsTokenListFlowUseCase.BatchFlowType.Search,
            order = TokenMarketListConfig.Order.ByRating,
            currentAppCurrency = Provider { selectedAppCurrencyFlow.value },
            currentSearchText = Provider { searchQueryStateForMarkets.value },
            modelScope = modelScope,
            dispatchers = dispatchers,
        )
    }

    private val visibleDefaultMarketItemIds = MutableStateFlow<List<CryptoCurrency.RawID>>(emptyList())

    init {
        subscribeOnUpdateState()

        initializeSearchBarCallbacks()
        subscribeOnSelectedStatusChange()
        subscribeOnAvailablePairsUpdates()

        if (swapFeatureToggles.isMarketListFeatureEnabled) {
            subscribeOnMarketsUpdates()
            subscribeOnVisibleMarketItems()
        }
    }

    private fun getAccountListUseCaseFlow(): SharedFlow<List<AccountStatus>> {
        return singleAccountStatusListSupplier(SingleAccountStatusListProducer.Params(params.userWalletId))
            .distinctUntilChanged()
            .mapNotNull { accountStatusList ->
                accountStatusList.accountStatuses.filter {
                    it is AccountStatus.CryptoPortfolio && it.tokenList !is TokenList.Empty
                }
            }
            .flowOn(dispatchers.default)
            .shareIn(scope = modelScope, started = SharingStarted.Eagerly, replay = 1)
    }

    private fun subscribeOnSelectedStatusChange() {
        params.selectedStatus
            .filter { it == null }
            .onEach { clearSearchState() }
            .launchIn(modelScope)
    }

    private fun initializeSearchBarCallbacks() {
        tokenListUMController.update(
            transformer = UpdateSearchBarCallbacksTransformer(
                onQueryChange = ::onSearchQueryChange,
                onActiveChange = ::onSearchBarActiveChange,
            ),
        )
    }

    private fun subscribeOnUpdateState() {
        combine(
            flow = getAccountsAndModeFlow(),
            flow2 = getAppCurrencyAndBalanceHidingFlow(),
            flow3 = params.selectedStatus,
            flow4 = searchManager.query,
            flow5 = availablePairsByNetworkFlow
                .map { it[params.selectedStatus.value?.toLeastTokenInfo()] }
                .distinctUntilChanged(),
        ) { accountListAndMode, appCurrencyAndBalanceHiding, selectedStatus, query, availablePairsState ->
            val (accountList, isAccountsMode) = accountListAndMode
            availablePairsState?.fold(
                ifLoading = {
                    SetLoadingAccountTokenListTransformer(
                        appCurrency = appCurrencyAndBalanceHiding.first,
                        accountList = accountList,
                        isAccountsMode = isAccountsMode,
                    )
                },
                ifContent = { pairs ->
                    handleContentState(
                        appCurrencyAndBalanceHiding = appCurrencyAndBalanceHiding,
                        accountList = accountList,
                        selectedStatus = selectedStatus,
                        query = query,
                        availablePairs = pairs,
                        isAccountsMode = isAccountsMode,
                    )
                },
                ifError = { throwable ->
                    handleErrorState(
                        cause = throwable,
                        networkInfo = params.selectedStatus.value?.toLeastTokenInfo(),
                        accountList = accountList,
                    )
                },
            ) ?: SetLoadingAccountTokenListTransformer(
                appCurrency = appCurrencyAndBalanceHiding.first,
                accountList = accountList,
                isAccountsMode = isAccountsMode,
            )
        }
            .onEach(tokenListUMController::update)
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun handleContentState(
        appCurrencyAndBalanceHiding: Pair<AppCurrency, Boolean>,
        accountList: List<AccountStatus>,
        selectedStatus: CryptoCurrencyStatus?,
        query: String,
        availablePairs: List<SwapPairLeast>,
        isAccountsMode: Boolean,
    ): TokenListUMTransformer {
        val (appCurrency, isBalanceHidden) = appCurrencyAndBalanceHiding

        val filterByQueryAccountList: Map<Account.CryptoPortfolio, List<CryptoCurrencyStatus>> = accountList
            .associate { accountStatus ->
                when (accountStatus) {
                    is AccountStatus.CryptoPortfolio -> {
                        val statuses = accountStatus.tokenList.flattenCurrencies()
                            .filterNot { status ->
                                status.currency.network.backendId == selectedStatus?.currency?.network?.backendId &&
                                    status.currency.id.contractAddress == selectedStatus.currency.id.contractAddress
                            }
                            .filterByQuery(query = query)

                        accountStatus.account to statuses
                    }
                    is AccountStatus.Payment -> TODO("[REDACTED_JIRA]")
                }
            }
            .filterValues { it.isNotEmpty() }

        if (availablePairs.isEmpty()) {
            return SetNoAvailablePairsTransformer(
                appCurrency = appCurrency,
                accountList = filterByQueryAccountList,
                unavailableErrorText = resourceReference(R.string.tokens_list_unavailable_to_swap_source_header),
                isBalanceHidden = isBalanceHidden,
                isAccountsMode = isAccountsMode,
            )
        }

        return if (query.isNotEmpty() && filterByQueryAccountList.isEmpty()) {
            SetNothingToFoundStateTransformer(
                isBalanceHidden = isBalanceHidden,
                emptySearchMessageReference = resourceReference(
                    id = R.string.action_buttons_swap_empty_search_message,
                ),
            )
        } else {
            UpdateAccountTokenListTransformer(
                appCurrency = appCurrency,
                onItemClick = ::onPortfolioTokenClick,
                accountList = filterByQueryAccountList.filterByAvailability(availablePairs = availablePairs),
                isBalanceHidden = isBalanceHidden,
                unavailableErrorText = resourceReference(R.string.tokens_list_unavailable_to_swap_source_header),
                isAccountsMode = isAccountsMode,
            )
        }
    }

    private fun handleErrorState(
        cause: Throwable,
        networkInfo: LeastTokenInfo?,
        accountList: List<AccountStatus>,
    ): SetErrorWarningTransformer {
        return SetErrorWarningTransformer(
            cause = cause,
            onRefresh = {
                modelScope.launch {
                    if (networkInfo != null) {
                        accountList.filterCryptoPortfolio()
                            .forEach { (_, currencies) ->
                                updateAvailablePairs(networkInfo, currencies.flattenCurrencies())
                            }
                    }
                }
            },
        )
    }

    private fun subscribeOnAvailablePairsUpdates() {
        modelScope.launch {
            combine(
                params.selectedStatus.filterNotNull(),
                refreshPairsTrigger
                    .onEach { availablePairsByNetworkFlow.value = emptyMap() }
                    .onStart { emit(Unit) },
            ) { status, _ -> status }
                .collectLatest { selectedStatus ->
                    val networkInfo = selectedStatus.toLeastTokenInfo()

                    val isAlreadyLoaded = availablePairsByNetworkFlow.value[networkInfo]?.isContent() == true
                    if (isAlreadyLoaded) return@collectLatest

                    val accountList = accountListFlow.firstOrNull() ?: return@collectLatest
                    updateAvailablePairs(
                        networkInfo = networkInfo,
                        statuses = accountList.filterCryptoPortfolio()
                            .flatMap { accountStatus ->
                                accountStatus.flattenCurrencies()
                            }.toSet().toList(),
                    )
                }
        }
    }

    private suspend fun updateAvailablePairs(networkInfo: LeastTokenInfo, statuses: List<CryptoCurrencyStatus>) {
        runSuspendCatching {
            availablePairsByNetworkFlow.update(networkInfo = networkInfo, state = lceLoading())

            getAvailablePairsUseCase(
                userWallet = userWallet,
                initialCurrency = networkInfo,
                currencies = statuses.map(CryptoCurrencyStatus::currency),
            )
        }
            .onSuccess { pairs ->
                availablePairsByNetworkFlow.update(networkInfo = networkInfo, state = pairs.lceContent())
            }
            .onFailure { cause ->
                availablePairsByNetworkFlow.update(networkInfo = networkInfo, state = cause.lceError())
            }
    }

    private fun MutableStateFlow<Map<LeastTokenInfo, AvailablePairsState>>.update(
        networkInfo: LeastTokenInfo,
        state: AvailablePairsState,
    ) {
        update { map ->
            map.toMutableMap().apply {
                this[networkInfo] = state
            }
        }
    }

    private fun getAppCurrencyAndBalanceHidingFlow(): Flow<Pair<AppCurrency, Boolean>> {
        return combine(
            flow = getSelectedAppCurrencyUseCase.invokeOrDefault(),
            flow2 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
            transform = ::Pair,
        )
    }

    private fun getAccountsAndModeFlow(): Flow<Pair<List<AccountStatus>, Boolean>> {
        return combine(
            flow = accountListFlow.distinctUntilChanged(),
            flow2 = isAccountsModeEnabledUseCase().distinctUntilChanged(),
            transform = ::Pair,
        )
    }

    private fun onSearchQueryChange(newQuery: String) {
        if (state.value.searchBarUM.query == newQuery) return

        modelScope.launch {
            tokenListUMController.update(transformer = UpdateSearchQueryTransformer(newQuery))

            searchManager.update(newQuery)

            searchQueryStateForMarkets.value = newQuery
        }
    }

    private fun onSearchBarActiveChange(isActive: Boolean) {
        tokenListUMController.update(
            transformer = UpdateSearchBarActiveStateTransformer(
                isActive = isActive,
                placeHolder = resourceReference(id = R.string.common_search),
            ),
        )
    }

    private fun List<CryptoCurrencyStatus>.filterByQuery(query: String): List<CryptoCurrencyStatus> {
        return filter { status ->
            status.currency.name.contains(other = query, ignoreCase = true) ||
                status.currency.symbol.contains(other = query, ignoreCase = true)
        }
    }

    private fun Map<Account.CryptoPortfolio, List<CryptoCurrencyStatus>>.filterByAvailability(
        availablePairs: List<SwapPairLeast>,
    ): List<AccountAvailabilityUM> {
        return map { (account, currencies) ->
            AccountAvailabilityUM(
                account = account,
                currencyList = currencies.map { status ->
                    val isAvailable = availablePairs.map(SwapPairLeast::to).contains(status.toLeastTokenInfo())

                    val isAvailableToSwap = isAvailable &&
                        status.value !is CryptoCurrencyStatus.MissedDerivation &&
                        status.value !is CryptoCurrencyStatus.Unreachable &&
                        !status.currency.isCustom

                    AccountCurrencyUM(
                        cryptoCurrencyStatus = status,
                        isAvailable = isAvailableToSwap,
                    )
                },
            )
        }
    }

    private fun onPortfolioTokenClick(tokenItem: TokenItemState, status: CryptoCurrencyStatus) {
        analyticsEventHandler.send(
            SwapAnalyticsEvent.TokenSelected(
                token = status.currency.symbol,
                source = ScreensSources.Portfolio,
                isSearched = state.value.searchBarUM.query.isNotEmpty(),
            ),
        )
        clearSearchState()
        params.onTokenClick(tokenItem, status)
    }

    private fun clearSearchState() {
        tokenListUMController.update(
            transformer = ClearSearchBarTransformer(
                placeHolder = resourceReference(id = R.string.common_search),
            ),
        )
        modelScope.launch {
            searchManager.update("")
        }
        searchQueryStateForMarkets.value = ""
    }

    private fun CryptoCurrencyStatus.toLeastTokenInfo(): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
            network = currency.network.backendId,
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun subscribeOnMarketsUpdates() {
        searchQueryStateForMarkets
            .map { it.isEmpty() }
            .distinctUntilChanged()
            .flatMapLatest { isDefaultMode ->
                if (isDefaultMode) {
                    visibleMarketItemIds.value = emptyList()
                    createDefaultMarketsFlow()
                } else {
                    visibleDefaultMarketItemIds.value = emptyList()
                    createSearchMarketsFlow()
                }
            }
            .onEach { marketsState ->
                tokenListUMController.update { it.copy(marketsState = marketsState) }
            }
            .flowOn(dispatchers.main)
            .launchIn(modelScope)

        searchQueryStateForMarkets
            .onEach { searchQuery ->
                if (searchQuery.isNotEmpty()) {
                    searchMarketsListManager.reload(searchQuery)
                }
            }
            .launchIn(modelScope)

        params.selectedStatus
            .filterNotNull()
            .take(1)
            .onEach { defaultMarketsListManager.reload() }
            .launchIn(modelScope)
    }

    private fun createDefaultMarketsFlow(): Flow<SwapMarketState> {
        val marketsTitle = TextReference.Res(CoreUiR.string.feed_trending_now)
        return combine(
            defaultMarketsListManager.uiItems,
            defaultMarketsListManager.isInInitialLoadingErrorState,
            defaultMarketsListManager.totalCount,
        ) { uiItems, isError, total ->
            when {
                isError -> SwapMarketState.LoadingError(
                    onRetryClicked = { defaultMarketsListManager.reload() },
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                )
                uiItems.isEmpty() -> SwapMarketState.Loading(
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                )
                else -> SwapMarketState.Content(
                    items = uiItems,
                    loadMore = { defaultMarketsListManager.loadMore() },
                    onItemClick = { item -> addToPortfolioItem(item) },
                    visibleIdsChanged = { visibleDefaultMarketItemIds.value = it },
                    total = total ?: uiItems.size,
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = false,
                )
            }
        }
    }

    private fun createSearchMarketsFlow(): Flow<SwapMarketState> {
        val marketsTitle = TextReference.Res(CoreUiR.string.markets_common_title)
        return combine(
            flow = searchMarketsListManager.uiItems,
            flow2 = searchMarketsListManager.isInInitialLoadingErrorState,
            flow3 = searchMarketsListManager.isSearchNotFoundState,
            flow4 = searchMarketsListManager.totalCount,
        ) { uiItems, isError, isSearchNotFound, total ->
            when {
                isError -> SwapMarketState.LoadingError(
                    onRetryClicked = {
                        searchMarketsListManager.reload(searchQueryStateForMarkets.value)
                    },
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = true,
                )
                isSearchNotFound -> SwapMarketState.SearchNothingFound
                uiItems.isEmpty() -> SwapMarketState.Loading(
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = true,
                )
                else -> SwapMarketState.Content(
                    items = uiItems,
                    loadMore = { searchMarketsListManager.loadMore() },
                    onItemClick = { item -> addToPortfolioItem(item) },
                    visibleIdsChanged = { visibleMarketItemIds.value = it },
                    total = total ?: uiItems.size,
                    marketsTitle = marketsTitle,
                    shouldAssetsCount = true,
                )
            }
        }
    }

    private fun onTokenAddedToPortfolio(addedToken: CryptoCurrency) {
        modelScope.launch {
            bottomSheetNavigation.dismiss()
            analyticsEventHandler.send(
                SwapAnalyticsEvent.TokenSelected(
                    token = addedToken.symbol,
                    source = ScreensSources.Markets,
                    isSearched = state.value.searchBarUM.query.isNotEmpty(),
                ),
            )

            clearSearchState()

            // Trigger re-fetch of available pairs (clears cache + re-enters collectLatest)
            refreshPairsTrigger.emit(Unit)

            // Wait for the added token status to become Loaded
            val addedTokenStatus = getAccountCurrencyStatusUseCase(params.userWalletId, addedToken)
                .firstOrNull { it.status.value is CryptoCurrencyStatus.Loaded }
                ?.status
                ?: return@launch

            // Convert to TokenItemState and trigger token selection → navigates to swap
            val converter = OnrampTokenItemStateConverterFactory.createAvailableItemConverter(
                appCurrency = selectedAppCurrencyFlow.value,
                onItemClick = params.onTokenClick,
            )
            params.onTokenClick(converter.convert(addedTokenStatus), addedTokenStatus)
        }
    }

    private fun addToPortfolioItem(item: MarketsListItemUM) {
        modelScope.launch {
            val tokenMarket = defaultMarketsListManager.getTokenMarketById(item.id)
                ?: searchMarketsListManager.getTokenMarketById(item.id)
                ?: return@launch

            val param = tokenMarket.toSerializableParam()
            val hasOnlyHotWallets = allUserWallets.all { it is UserWallet.Hot }

            val networks = tokenMarket.networks?.filter { network ->
                BlockchainUtils.isSupportedNetworkId(
                    blockchainId = network.networkId,
                    coinId = tokenMarket.id.value,
                    contractAddress = network.contractAddress,
                    excludedBlockchains = excludedBlockchains,
                    hotExcludedBlockchains = hotWalletExcludedBlockchains,
                    hasOnlyHotWallets = hasOnlyHotWallets,
                )
            }?.map { network ->
                TokenMarketInfo.Network(
                    networkId = network.networkId,
                    isExchangeable = false,
                    contractAddress = network.contractAddress,
                    decimalCount = network.decimalCount,
                )
            }.orEmpty()

            addToPortfolioManager = addToPortfolioManagerFactory
                .create(
                    scope = modelScope,
                    token = param,
                    analyticsParams = AddToPortfolioManager.AnalyticsParams(source = ScreensSources.Swap.value),
                ).apply {
                    setTokenNetworks(networks)
                }

            addToPortfolioManager?.state
                ?.firstOrNull { it is AddToPortfolioManager.State.AvailableToAdd }
                ?.run { bottomSheetNavigation.activate(AddToPortfolioRoute) }
        }.saveIn(addToPortfolioJobHolder)
    }

    private fun subscribeOnVisibleMarketItems() {
        modelScope.launch {
            visibleMarketItemIds.mapNotNull { rawIds ->
                if (rawIds.isNotEmpty()) {
                    searchMarketsListManager.getBatchKeysByItemIds(rawIds)
                } else {
                    null
                }
            }.distinctUntilChanged().collectLatest { visibleBatchKeys ->
                searchMarketsListManager.loadCharts(visibleBatchKeys)
            }
        }
        modelScope.launch {
            visibleDefaultMarketItemIds.mapNotNull { rawIds ->
                if (rawIds.isNotEmpty()) {
                    defaultMarketsListManager.getBatchKeysByItemIds(rawIds)
                } else {
                    null
                }
            }.distinctUntilChanged().collectLatest { visibleBatchKeys ->
                defaultMarketsListManager.loadCharts(visibleBatchKeys)
            }
        }
    }
}