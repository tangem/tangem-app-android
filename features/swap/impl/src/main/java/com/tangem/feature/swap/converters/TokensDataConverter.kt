package com.tangem.feature.swap.converters

import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup
import com.tangem.feature.swap.models.SwapSelectTokenStateHolder
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.models.market.state.SwapMarketState
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentList

@Suppress("LongParameterList")
internal class TokensDataConverter(
    private val onSearchEntered: (String) -> Unit,
    onTokenClick: (String) -> Unit,
    onAccountClick: (Account.CryptoPortfolio) -> Unit,
    private val expandedAccounts: Map<AccountId, Boolean>,
    private val tokensDataState: CurrenciesGroup,
    private val isBalanceHidden: Boolean,
    private val isAccountsMode: Boolean,
    private val appCurrency: AppCurrency,
    private val marketState: SwapMarketState,
) {

    private val accountListItemConverter = AccountTokenItemConverter(
        appCurrency = appCurrency,
        unavailableErrorText = resourceReference(R.string.tokens_list_unavailable_to_swap_source_header),
        onTokenItemClick = onTokenClick,
        onAccountItemClick = onAccountClick,
        expandedAccounts = expandedAccounts,
    )

    fun transform(): SwapSelectTokenStateHolder {
        val accountList = tokensDataState.accountCurrencyList
        return SwapSelectTokenStateHolder(
            tokensListData = if (isAccountsMode) {
                val portfolioList = accountListItemConverter.convertList(accountList).toPersistentList()
                val totalTokensCount = portfolioList.sumOf { it.tokens.size }
                if (totalTokensCount > 0) {
                    TokenListUMData.AccountList(
                        tokensList = portfolioList,
                        totalTokensCount = totalTokensCount,
                    )
                } else {
                    TokenListUMData.EmptyList
                }
            } else {
                val tokensList = accountList.flatMap { (_, currencyList) ->
                    currencyList.asSequence().map { accountSwapCurrency ->
                        accountListItemConverter.createAvailableItemConverter()
                            .convert(accountSwapCurrency.cryptoCurrencyStatus)
                    }.map(TokensListItemUM::Token).toPersistentList()
                }.toPersistentList()

                if (tokensList.isNotEmpty()) {
                    TokenListUMData.TokenList(
                        tokensList = persistentListOf(
                            TokensListItemUM.GroupTitle(
                                id = "available_tokens_title",
                                text = resourceReference(R.string.exchange_tokens_available_tokens_header),
                            ),
                        ) + tokensList,
                        totalTokensCount = tokensList.size,
                    )
                } else {
                    TokenListUMData.EmptyList
                }
            },
            marketsState = marketState,
            onSearchEntered = onSearchEntered,
            isBalanceHidden = isBalanceHidden,
            isAfterSearch = tokensDataState.isAfterSearch,
        )
    }
}