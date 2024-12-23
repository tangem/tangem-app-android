package com.tangem.features.onramp.tokenlist.model

import arrow.core.getOrElse
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.exchange.RampStateManager
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.entity.OnrampOperation
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMController
import com.tangem.features.onramp.tokenlist.entity.transformer.SetNothingToFoundStateTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.UpdateTokenItemsTransformer
import com.tangem.features.onramp.utils.InputManager
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarCallbacksTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class OnrampTokenListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tokenListUMController: TokenListUMController,
    private val searchManager: InputManager,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val rampStateManager: RampStateManager,
) : Model() {

    val state: StateFlow<TokenListUM> = tokenListUMController.state

    private val params: OnrampTokenListComponent.Params = paramsContainer.require()
    private val scanResponse by lazy {
        getWalletsUseCase.invokeSync().first { it.walletId == params.userWalletId }.scanResponse
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
            flow = getTokenListUseCase.launch(userWalletId = params.userWalletId).distinctUntilChanged(),
            flow2 = getSelectedAppCurrencyUseCase().map { it.getOrElse { AppCurrency.Default } }.distinctUntilChanged(),
            flow3 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
            flow4 = searchManager.query,
        ) { maybeTokenList, appCurrency, isBalanceHidden, query ->
            val currencies = maybeTokenList.getOrElse(
                ifLoading = { it ?: TokenList.Empty },
                ifError = { TokenList.Empty },
            )
                .flattenCurrencies()

            val filterByQueryTokenList = currencies
                .filterByQuery(query = query)

            if (query.isNotEmpty() && filterByQueryTokenList.isEmpty()) {
                SetNothingToFoundStateTransformer(
                    isBalanceHidden = isBalanceHidden,
                    emptySearchMessageReference = when (params.filterOperation) {
                        OnrampOperation.BUY -> R.string.action_buttons_buy_empty_search_message
                        OnrampOperation.SELL -> R.string.action_buttons_sell_empty_search_message
                        OnrampOperation.SWAP -> R.string.action_buttons_swap_empty_search_message
                    }
                        .let(::resourceReference),
                )
            } else {
                UpdateTokenItemsTransformer(
                    appCurrency = appCurrency,
                    onItemClick = params.onTokenClick,
                    statuses = filterByQueryTokenList.filterByAvailability(),
                    isBalanceHidden = isBalanceHidden,
                    unavailableTokensHeaderReference = when (params.filterOperation) {
                        OnrampOperation.BUY -> R.string.tokens_list_unavailable_to_purchase_header
                        OnrampOperation.SELL -> R.string.tokens_list_unavailable_to_sell_header
                        OnrampOperation.SWAP -> R.string.tokens_list_unavailable_to_swap_source_header
                    }.let(::resourceReference),
                )
            }
        }
            .onEach(tokenListUMController::update)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
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

    private fun List<CryptoCurrencyStatus>.filterByQuery(query: String): List<CryptoCurrencyStatus> {
        return filter {
            it.currency.name.contains(other = query, ignoreCase = true) ||
                it.currency.symbol.contains(other = query, ignoreCase = true)
        }
    }

    private suspend fun List<CryptoCurrencyStatus>.filterByAvailability(): Map<Boolean, List<CryptoCurrencyStatus>> {
        return coroutineScope {
            map { status ->
                async {
                    val isAvailable = checkAvailabilityByOperation(status = status)
                    val isNotMissedDerivation = status.value !is CryptoCurrencyStatus.MissedDerivation
                    val isNotLoading = status.value !is CryptoCurrencyStatus.Loading

                    val isNotUnreachable = when (params.filterOperation) {
                        OnrampOperation.BUY -> true // unreachable state is available for Buy operation
                        OnrampOperation.SELL -> status.value !is CryptoCurrencyStatus.Unreachable
                        OnrampOperation.SWAP -> status.value !is CryptoCurrencyStatus.Unreachable
                    }

                    status to (isAvailable && isNotMissedDerivation && isNotLoading && isNotUnreachable)
                }
            }
                .awaitAll()
                .groupBy(Pair<CryptoCurrencyStatus, Boolean>::second)
                .mapValues { it.value.map(Pair<CryptoCurrencyStatus, Boolean>::first) }
        }
    }

    private suspend fun checkAvailabilityByOperation(status: CryptoCurrencyStatus): Boolean {
        return when (params.filterOperation) {
            OnrampOperation.BUY -> {
                rampStateManager.availableForBuy(
                    scanResponse = scanResponse,
                    userWalletId = params.userWalletId,
                    cryptoCurrency = status.currency,
                )
            }
            OnrampOperation.SELL -> {
                rampStateManager.availableForSell(userWalletId = params.userWalletId, status = status)
            }
            OnrampOperation.SWAP -> {
                val isAvailable = rampStateManager.availableForSwap(
                    userWalletId = params.userWalletId,
                    cryptoCurrency = status.currency,
                )

                isAvailable && status.value !is CryptoCurrencyStatus.NoQuote
            }
        }
    }
}