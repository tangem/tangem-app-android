package com.tangem.feature.wallet.presentation.tokenlist.model

import arrow.core.getOrElse
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.wallet.presentation.tokenlist.TokenListComponent
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUM
import com.tangem.feature.wallet.presentation.tokenlist.entity.TokenListUMController
import com.tangem.feature.wallet.presentation.tokenlist.entity.transformer.UpdateSearchBarActiveStateTransformer
import com.tangem.feature.wallet.presentation.tokenlist.entity.transformer.UpdateSearchQueryTransformer
import com.tangem.feature.wallet.presentation.tokenlist.entity.transformer.UpdateTokenItemsTransformer
import com.tangem.feature.wallet.presentation.tokenlist.utils.SearchTokensManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
internal class TokenListModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tokenListUMController: TokenListUMController,
    private val searchTokensManager: SearchTokensManager,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
) : Model() {

    val state: StateFlow<TokenListUM> = tokenListUMController.state

    private val params: TokenListComponent.Params = paramsContainer.require()

    init {
        subscribeOnUpdateState()
    }

    private fun subscribeOnUpdateState() {
        combine(
            flow = getTokenListUseCase.launch(userWalletId = params.userWalletId).distinctUntilChanged(),
            flow2 = getSelectedAppCurrencyUseCase().map { it.getOrElse { AppCurrency.Default } }.distinctUntilChanged(),
            flow3 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
            flow4 = searchTokensManager.query,
        ) { maybeTokenList, appCurrency, isBalanceHidden, query ->
            val currencies = maybeTokenList.getOrElse(
                ifLoading = { it ?: TokenList.Empty },
                ifError = { TokenList.Empty },
            )
                .flattenCurrencies()

            val filterTokenList = currencies
                .filterByQuery(query = query)
                .filterByAvailability()

            UpdateTokenItemsTransformer(
                tokenItemStateConverter = TokenItemStateConverter(
                    appCurrency = appCurrency,
                    onItemClick = params.onTokenClick,
                ),
                statuses = filterTokenList,
                isBalanceHidden = isBalanceHidden,
                hasSearchBar = params.hasSearchBar && currencies.isNotEmpty(),
                onQueryChange = ::onSearchQueryChange,
                onActiveChange = ::onSearchBarActiveChange,
            )
        }
            .onEach(tokenListUMController::update)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun onSearchQueryChange(newQuery: String) {
        val searchBar = tokenListUMController.getSearchBar()
        if (searchBar?.searchBarUM?.query == newQuery) return

        modelScope.launch {
            tokenListUMController.update(transformer = UpdateSearchQueryTransformer(newQuery))

            searchTokensManager.update(newQuery)
        }
    }

    private fun onSearchBarActiveChange(isActive: Boolean) {
        tokenListUMController.update(transformer = UpdateSearchBarActiveStateTransformer(isActive))
    }

    private fun List<CryptoCurrencyStatus>.filterByQuery(query: String): List<CryptoCurrencyStatus> {
        return filter {
            it.currency.name.contains(other = query, ignoreCase = true) ||
                it.currency.symbol.contains(other = query, ignoreCase = true)
        }
    }
// [REDACTED_TODO_COMMENT]
    private fun List<CryptoCurrencyStatus>.filterByAvailability(): List<CryptoCurrencyStatus> {
        return this
    }
}
