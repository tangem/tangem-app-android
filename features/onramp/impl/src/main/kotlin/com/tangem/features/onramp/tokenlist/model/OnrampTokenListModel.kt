package com.tangem.features.onramp.tokenlist.model

import arrow.core.getOrElse
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.settings.usercountry.GetUserCountryUseCase
import com.tangem.domain.settings.usercountry.models.UserCountry
import com.tangem.domain.tokens.GetAssetRequirementsUseCase
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.entity.AccountAvailabilityUM
import com.tangem.features.onramp.swap.entity.AccountCurrencyUM
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.entity.*
import com.tangem.features.onramp.tokenlist.entity.transformer.SetLoadingAccountTokenListTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.SetNothingToFoundStateTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.UpdateAccountTokenListTransformer
import com.tangem.features.onramp.utils.ClearSearchBarTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarCallbacksTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.isZero
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias AccountCryptoList = Map<Account.CryptoPortfolio, List<CryptoCurrencyStatus>>

@Suppress("LargeClass", "LongParameterList")
internal class OnrampTokenListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tokenListUMController: TokenListUMController,
    private val searchManager: InputManager,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val rampStateManager: RampStateManager,
    private val getUserCountryUseCase: GetUserCountryUseCase,
    private val getAssetRequirementsUseCase: GetAssetRequirementsUseCase,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
) : Model() {

    val state: StateFlow<TokenListUM> = tokenListUMController.state

    private val params: OnrampTokenListComponent.Params = paramsContainer.require()
    private val userWallet by lazy {
        getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }
    }

    init {
        tokenListUMController.update(
            transformer = UpdateSearchBarCallbacksTransformer(
                onQueryChange = ::onSearchQueryChange,
                onActiveChange = ::onSearchBarActiveChange,
            ),
        )
        subscribeOnUpdateState()
    }

    private fun subscribeOnUpdateState() {
        combine(
            flow = singleAccountStatusListSupplier(
                SingleAccountStatusListProducer.Params(params.userWalletId),
            ).distinctUntilChanged(),
            flow2 = getAppCurrencyAndBalanceHidingFlow(),
            flow3 = isAccountsModeEnabledUseCase(),
            flow4 = searchManager.query,
            flow5 = hasRestrictionForSellFlow(),
        ) { accountList, appCurrencyAndBalanceHiding, isAccountsMode, query, hasRestrictionForSell ->
            val (appCurrency, isBalanceHidden) = appCurrencyAndBalanceHiding
            val filterByQueryAccountList = accountList.filterAccountsByQuery(query)

            if (query.isNotEmpty() && filterByQueryAccountList.isEmpty()) {
                updateTokenListUM(
                    SetNothingToFoundStateTransformer(
                        isBalanceHidden = isBalanceHidden,
                        emptySearchMessageReference = getEmptySearchMessageReference(),
                    ),
                )
            } else {
                updateTokenListUM(
                    SetLoadingAccountTokenListTransformer(
                        appCurrency = appCurrency,
                        accountList = accountList.accountStatuses.toList(),
                        isAccountsMode = isAccountsMode,
                    ),
                )
                updateTokenListUM(
                    UpdateAccountTokenListTransformer(
                        appCurrency = appCurrency,
                        onItemClick = ::onTokenClick,
                        accountList = filterByQueryAccountList.filterByAvailability(),
                        isBalanceHidden = isBalanceHidden,
                        unavailableErrorText = getUnavailableTokensHeaderReference(),
                        warning = getSellWarning(
                            hasRestrictionForSell = hasRestrictionForSell,
                            isInsufficientBalanceForSell = accountList.isInsufficientBalanceForSell(),
                        ),
                        isAccountsMode = isAccountsMode,
                    ),
                )
            }
        }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun getAppCurrencyAndBalanceHidingFlow(): Flow<Pair<AppCurrency, Boolean>> {
        return combine(
            flow = getSelectedAppCurrencyUseCase().map { it.getOrElse { AppCurrency.Default } }.distinctUntilChanged(),
            flow2 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
            transform = ::Pair,
        )
    }

    private fun hasRestrictionForSellFlow(): Flow<Boolean> {
        return if (params.filterOperation == OnrampOperation.SELL) {
            getUserCountryUseCase().map { maybe ->
                maybe.isRight { country -> country is UserCountry.Russia }
            }
        } else {
            flowOf(false)
        }
    }

    private fun AccountStatusList.isInsufficientBalanceForSell(): Boolean {
        return if (params.filterOperation == OnrampOperation.SELL) {
            (totalFiatBalance as? TotalFiatBalance.Loaded)?.amount?.isZero() == true
        } else {
            false
        }
    }

    private fun getUnavailableTokensHeaderReference(): TextReference {
        val res = when (params.filterOperation) {
            OnrampOperation.BUY -> R.string.tokens_list_unavailable_to_purchase_header
            OnrampOperation.SELL -> R.string.tokens_list_unavailable_to_sell_header
            OnrampOperation.SWAP -> R.string.tokens_list_unavailable_to_swap_source_header
        }

        return resourceReference(res)
    }

    private fun getEmptySearchMessageReference(): TextReference {
        val res = when (params.filterOperation) {
            OnrampOperation.BUY -> R.string.action_buttons_buy_empty_search_message
            OnrampOperation.SELL -> R.string.action_buttons_sell_empty_search_message
            OnrampOperation.SWAP -> R.string.action_buttons_swap_empty_search_message
        }

        return resourceReference(res)
    }

    private fun updateTokenListUM(transformer: TokenListUMTransformer) {
        modelScope.launch {
            tokenListUMController.update { prevState ->
                transformer.transform(prevState).apply {
                    if (isFirstInitialization(prevState = prevState, newState = this)) {
                        params.onTokenListInitialized()
                    }
                }
            }
        }
    }

    private fun getSellWarning(hasRestrictionForSell: Boolean, isInsufficientBalanceForSell: Boolean) = when {
        hasRestrictionForSell -> NotificationUM.Warning.SellingRegionalRestriction
        isInsufficientBalanceForSell -> NotificationUM.Warning.InsufficientBalanceForSelling
        else -> null
    }

    private fun isFirstInitialization(prevState: TokenListUM, newState: TokenListUM): Boolean {
        return prevState.tokensListData == TokenListUMData.EmptyList &&
            newState.tokensListData != TokenListUMData.EmptyList
    }

    private fun onTokenClick(tokenItemState: TokenItemState, status: CryptoCurrencyStatus) {
        clearSearchState()
        params.onTokenClick(tokenItemState, status)
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
    }

    private fun onSearchQueryChange(newQuery: String) {
        val searchBar = state.value.searchBarUM
        if (searchBar.query == newQuery) return

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

    private fun AccountStatusList.filterAccountsByQuery(
        query: String,
    ): Map<Account.CryptoPortfolio, List<CryptoCurrencyStatus>> = accountStatuses.asSequence()
        .associate { accountStatus ->
            when (accountStatus) {
                is AccountStatus.CryptoPortfolio -> {
                    val filteredList = accountStatus.tokenList.flattenCurrencies().filterByQuery(query = query)
                    accountStatus.account to filteredList
                }
                is AccountStatus.Payment -> TODO("[REDACTED_JIRA]")
            }
        }.filter { (_, value) -> value.isNotEmpty() }

    private fun List<CryptoCurrencyStatus>.filterByQuery(query: String): List<CryptoCurrencyStatus> {
        return filter { status ->
            status.currency.name.contains(other = query, ignoreCase = true) ||
                status.currency.symbol.contains(other = query, ignoreCase = true)
        }
    }

    private suspend fun AccountCryptoList.filterByAvailability(): List<AccountAvailabilityUM> {
        return coroutineScope {
            map { (account, currencies) ->
                async {
                    AccountAvailabilityUM(
                        account = account,
                        currencyList = currencies.map { status ->
                            val isOperationAvailable = checkAvailabilityByOperation(status = status)
                            val isNotMissedDerivation = status.value !is CryptoCurrencyStatus.MissedDerivation
                            val isNotLoading = status.value !is CryptoCurrencyStatus.Loading

                            val requirements = getAssetRequirementsUseCase(
                                userWalletId = userWallet.walletId,
                                currency = status.currency,
                            ).getOrNull()

                            val isAvailableForBuy = rampStateManager.checkAssetRequirements(requirements)
                            val isNotUnreachable = status.value !is CryptoCurrencyStatus.Unreachable

                            val isAvailable = when (params.filterOperation) {
                                OnrampOperation.BUY -> {
                                    isAvailableForBuy
                                } // unreachable state is available for Buy operation
                                OnrampOperation.SELL -> isNotUnreachable
                                OnrampOperation.SWAP -> {
                                    isNotUnreachable && isAvailableForBuy
                                }
                            }

                            val isTotalAvailable =
                                isOperationAvailable && isNotMissedDerivation && isNotLoading && isAvailable

                            AccountCurrencyUM(
                                cryptoCurrencyStatus = status,
                                isAvailable = isTotalAvailable,
                            )
                        },
                    )
                }
            }.awaitAll()
        }
    }

    private suspend fun checkAvailabilityByOperation(status: CryptoCurrencyStatus): Boolean {
        return when (params.filterOperation) {
            OnrampOperation.BUY -> {
                rampStateManager.availableForBuy(
                    userWallet = userWallet,
                    cryptoCurrency = status.currency,
                ).isAvailable()
            }
            OnrampOperation.SELL -> {
                rampStateManager.availableForSell(
                    userWalletId = userWallet.walletId,
                    status = status,
                    sendUnavailabilityReason = null,
                ).isRight()
            }
            OnrampOperation.SWAP -> {
                val isAvailable = rampStateManager.availableForSwap(
                    userWalletId = params.userWalletId,
                    cryptoCurrency = status.currency,
                ).isAvailable() && !status.currency.isCustom

                val supplyStatus = status.value.yieldSupplyStatus
                val isUnavailableByYieldSupply = supplyStatus?.isAllowedToSpend == false && supplyStatus.isActive

                isAvailable && status.value !is CryptoCurrencyStatus.NoQuote && !isUnavailableByYieldSupply
            }
        }
    }

    private fun ScenarioUnavailabilityReason.isAvailable(): Boolean {
        return this == ScenarioUnavailabilityReason.None
    }
}