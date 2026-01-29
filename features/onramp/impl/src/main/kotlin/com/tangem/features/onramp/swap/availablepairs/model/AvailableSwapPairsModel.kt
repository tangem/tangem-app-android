package com.tangem.features.onramp.swap.availablepairs.model

import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.extensions.capitalize
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.swap.domain.GetAvailablePairsUseCase
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.availablepairs.AvailableSwapPairsComponent
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetErrorWarningTransformer
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetLoadingTokenItemsTransformer
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetNoAvailablePairsTransformer
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetNoAvailablePairsTransformerV2
import com.tangem.features.onramp.swap.entity.AccountAvailabilityUM
import com.tangem.features.onramp.swap.entity.AccountCurrencyUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMController
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.*
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarCallbacksTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private typealias AvailablePairsState = Lce<Throwable, List<SwapPairLeast>>

@Suppress("LongParameterList", "LargeClass")
internal class AvailableSwapPairsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val tokenListUMController: TokenListUMController,
    private val searchManager: InputManager,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getAvailablePairsUseCase: GetAvailablePairsUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val accountsFeatureToggles: AccountsFeatureToggles,
    getWalletsUseCase: GetWalletsUseCase,
) : Model() {

    val state: StateFlow<TokenListUM> = tokenListUMController.state

    private val params: AvailableSwapPairsComponent.Params = paramsContainer.require()
    private val userWallet = getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }

    private val tokenListFlow = getTokenListUseCaseFlow()
    private val accountListFlow = getAccountListUseCaseFlow()
    private val availablePairsByNetworkFlow = MutableStateFlow<Map<LeastTokenInfo, AvailablePairsState>>(emptyMap())

    init {
        if (accountsFeatureToggles.isFeatureEnabled) {
            subscribeOnUpdateStateV2()
        } else {
            subscribeOnUpdateState()
        }

        initializeSearchBarCallbacks()
        subscribeOnAvailablePairsUpdates()
    }

    private fun getTokenListUseCaseFlow(): SharedFlow<List<CryptoCurrencyStatus>> {
        return getTokenListUseCase.launch(userWalletId = params.userWalletId)
            .distinctUntilChanged()
            .map { maybeTokenList ->
                maybeTokenList.getOrElse(
                    ifLoading = { it ?: TokenList.Empty },
                    ifError = { TokenList.Empty },
                ).flattenCurrencies()
            }
            .shareIn(scope = modelScope, started = SharingStarted.Eagerly, replay = 1)
    }

    private fun getAccountListUseCaseFlow(): SharedFlow<List<AccountStatus>> {
        return singleAccountStatusListSupplier(SingleAccountStatusListProducer.Params(params.userWalletId))
            .distinctUntilChanged()
            .mapNotNull { accountStatusList ->
                accountStatusList.accountStatuses.filter {
                    it is AccountStatus.Crypto.Portfolio && it.tokenList !is TokenList.Empty
                }
            }
            .flowOn(dispatchers.default)
            .shareIn(scope = modelScope, started = SharingStarted.Eagerly, replay = 1)
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
            flow = tokenListFlow,
            flow2 = getAppCurrencyAndBalanceHidingFlow(),
            flow3 = params.selectedStatus,
            flow4 = searchManager.query,
            flow5 = availablePairsByNetworkFlow
                .map { it[params.selectedStatus.value?.toLeastTokenInfo()] }
                .distinctUntilChanged(),
        ) { currencies, appCurrencyAndBalanceHiding, selectedStatus, query, availablePairsState ->
            availablePairsState?.fold(
                ifLoading = { SetLoadingTokenItemsTransformer(currencies) },
                ifContent = { pairs ->
                    handleContentState(
                        appCurrencyAndBalanceHiding = appCurrencyAndBalanceHiding,
                        currencies = currencies,
                        selectedStatus = selectedStatus,
                        query = query,
                        availablePairs = pairs,
                    )
                },
                ifError = { throwable ->
                    handleErrorState(
                        cause = throwable,
                        networkInfo = params.selectedStatus.value?.toLeastTokenInfo(),
                        currencies = currencies,
                    )
                },
            )
                ?: SetLoadingTokenItemsTransformer(currencies)
        }
            .onEach(tokenListUMController::update)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun subscribeOnUpdateStateV2() {
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
                    handleContentStateV2(
                        appCurrencyAndBalanceHiding = appCurrencyAndBalanceHiding,
                        accountList = accountList,
                        selectedStatus = selectedStatus,
                        query = query,
                        availablePairs = pairs,
                        isAccountsMode = isAccountsMode,
                    )
                },
                ifError = { throwable ->
                    handleErrorStateV2(
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
        currencies: List<CryptoCurrencyStatus>,
        selectedStatus: CryptoCurrencyStatus?,
        query: String,
        availablePairs: List<SwapPairLeast>,
    ): TokenListUMTransformer {
        val (appCurrency, isBalanceHidden) = appCurrencyAndBalanceHiding

        if (availablePairs.isEmpty()) {
            return SetNoAvailablePairsTransformer(
                appCurrency = appCurrency,
                unavailableStatuses = currencies,
                isBalanceHidden = isBalanceHidden,
                unavailableTokensHeaderReference = resourceReference(
                    id = R.string.tokens_list_unavailable_to_swap_header,
                    wrappedList(selectedStatus?.currency?.name?.capitalize().orEmpty()),
                ),
            )
        }

        val filterByQueryTokenList = currencies
            .filter { it.currency != selectedStatus?.currency }
            .filterByQuery(query = query)

        return if (query.isNotEmpty() && filterByQueryTokenList.isEmpty()) {
            SetNothingToFoundStateTransformer(
                isBalanceHidden = isBalanceHidden,
                emptySearchMessageReference = resourceReference(
                    id = R.string.action_buttons_swap_empty_search_message,
                ),
            )
        } else {
            UpdateTokenItemsTransformer(
                appCurrency = appCurrency,
                onItemClick = params.onTokenClick,
                statuses = filterByQueryTokenList.filterByAvailability(availablePairs = availablePairs),
                isBalanceHidden = isBalanceHidden,
                unavailableTokensHeaderReference = resourceReference(
                    id = R.string.tokens_list_unavailable_to_swap_header,
                    wrappedList(selectedStatus?.currency?.name?.capitalize().orEmpty()),
                ),
            )
        }
    }

    private fun handleContentStateV2(
        appCurrencyAndBalanceHiding: Pair<AppCurrency, Boolean>,
        accountList: List<AccountStatus>,
        selectedStatus: CryptoCurrencyStatus?,
        query: String,
        availablePairs: List<SwapPairLeast>,
        isAccountsMode: Boolean,
    ): TokenListUMTransformer {
        val (appCurrency, isBalanceHidden) = appCurrencyAndBalanceHiding

        val filterByQueryAccountList: Map<Account.Crypto, List<CryptoCurrencyStatus>> = accountList
            .associate { accountStatus ->
                when (accountStatus) {
                    is AccountStatus.Crypto.Portfolio -> {
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
            return SetNoAvailablePairsTransformerV2(
                appCurrency = appCurrency,
                accountList = filterByQueryAccountList,
                unavailableErrorText = resourceReference(R.string.tokens_list_unavailable_to_swap_source_header),
                isBalanceHidden = isBalanceHidden,
                isAccountsMode = isAccountsMode,
            )
        }

        return if (query.isNotEmpty() && filterByQueryAccountList.isEmpty()) {
            SetNothingToFoundStateTransformerV2(
                isBalanceHidden = isBalanceHidden,
                emptySearchMessageReference = resourceReference(
                    id = R.string.action_buttons_swap_empty_search_message,
                ),
            )
        } else {
            UpdateAccountTokenListTransformer(
                appCurrency = appCurrency,
                onItemClick = params.onTokenClick,
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
        currencies: List<CryptoCurrencyStatus>,
    ): SetErrorWarningTransformer {
        return SetErrorWarningTransformer(
            cause = cause,
            onRefresh = {
                modelScope.launch {
                    if (networkInfo != null) {
                        updateAvailablePairs(networkInfo, currencies)
                    }
                }
            },
        )
    }

    private fun handleErrorStateV2(
        cause: Throwable,
        networkInfo: LeastTokenInfo?,
        accountList: List<AccountStatus>,
    ): SetErrorWarningTransformer {
        return SetErrorWarningTransformer(
            cause = cause,
            onRefresh = {
                modelScope.launch {
                    if (networkInfo != null) {
                        accountList.filterIsInstance<AccountStatus.Crypto.Portfolio>()
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
            params.selectedStatus
                .filterNotNull()
                .collectLatest { selectedStatus ->
                    val networkInfo = selectedStatus.toLeastTokenInfo()

                    val isAlreadyLoaded = availablePairsByNetworkFlow.value[networkInfo]?.isContent() == true
                    if (isAlreadyLoaded) return@collectLatest

                    if (accountsFeatureToggles.isFeatureEnabled) {
                        val accountList = accountListFlow.firstOrNull() ?: return@collectLatest
                        updateAvailablePairs(
                            networkInfo = networkInfo,
                            statuses = accountList.filterIsInstance<AccountStatus.Crypto.Portfolio>()
                                .flatMap { accountStatus ->
                                    accountStatus.flattenCurrencies()
                                }.toSet().toList(),
                        )
                    } else {
                        val statuses = tokenListFlow.firstOrNull() ?: return@collectLatest
                        updateAvailablePairs(networkInfo = networkInfo, statuses = statuses)
                    }
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

    private fun List<CryptoCurrencyStatus>.filterByAvailability(
        availablePairs: List<SwapPairLeast>,
    ): Map<Boolean, List<CryptoCurrencyStatus>> {
        return groupBy { status ->
            val isAvailable = availablePairs.map(SwapPairLeast::to).contains(status.toLeastTokenInfo())

            isAvailable &&
                status.value !is CryptoCurrencyStatus.MissedDerivation &&
                status.value !is CryptoCurrencyStatus.Unreachable &&
                !status.currency.isCustom
        }
    }

    private fun Map<Account.Crypto, List<CryptoCurrencyStatus>>.filterByAvailability(
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

    private fun CryptoCurrencyStatus.toLeastTokenInfo(): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
            network = currency.network.backendId,
        )
    }
}