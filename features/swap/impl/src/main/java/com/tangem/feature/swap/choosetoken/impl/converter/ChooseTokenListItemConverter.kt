package com.tangem.feature.swap.choosetoken.impl.converter

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.common.ui.account.TokensListPortfolioItemConverter
import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.common.ui.tokens.TokenItemGrouping.toGroupedItems
import com.tangem.common.ui.tokens.TokenItemGrouping.toUngroupedItems
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.swap.choosetoken.impl.model.ClickIntents
import com.tangem.feature.swap.choosetoken.impl.model.isSearchingState
import com.tangem.feature.swap.models.TokenListUMData
import kotlinx.collections.immutable.toPersistentList

internal class ChooseTokenListItemConverter(
    private val appCurrency: AppCurrency,
    private val params: TokenConverterParams,
    private val clickIntents: ClickIntents,
    private val searchQuery: String,
) {

    private val isSearchingState: Boolean get() = searchQuery.isSearchingState

    private val onTokenClick: (account: AccountStatus, currencyStatus: CryptoCurrencyStatus) -> Unit =
        { account, currencyStatus ->
            clickIntents.onTokenItemClick(account, currencyStatus)
        }

    private fun tokenStatusConverter(account: AccountStatus) = TokenItemStateConverter(
        appCurrency = appCurrency,
        onItemClick = { _, status -> onTokenClick(account, status) },
    )

    fun convert(): TokenListUMData {
        return when (params) {
            is TokenConverterParams.Account -> convertAccountList(params)
            is TokenConverterParams.Wallet -> convertTokenList(
                tokenConverter = tokenStatusConverter(params.mainAccount),
                tokenListParam = params.tokenList,
            )
        }
    }

    private fun convertAccountList(params: TokenConverterParams.Account): TokenListUMData {
        val accountList = params.accountList
        val accountItems = accountList.accountStatuses
            .filterCryptoPortfolio()
            .map { accountStatus -> accountStatus.toPortfolioItem(params) }
            .filter { portfolio -> portfolio.tokens.isNotEmpty() }
        if (accountItems.isEmpty()) {
            return TokenListUMData.EmptyList
        }
        val accountsList = accountItems.toPersistentList()
        return TokenListUMData.AccountList(
            tokensList = accountsList,
            totalTokensCount = accountsList.size,
        )
    }

    private fun AccountStatus.CryptoPortfolio.toPortfolioItem(
        params: TokenConverterParams.Account,
    ): TokensListItemUM.Portfolio {
        val tokenList: TokenList = this.tokenList
        val account: Account.CryptoPortfolio = this.account
        val isExpanded = isSearchingState || params.expandedAccounts.contains(account.accountId)
        val onItemClick: (Account.CryptoPortfolio) -> Unit = { clickedAccount ->
            if (isExpanded) {
                clickIntents.onAccountCollapseClick(clickedAccount)
            } else {
                clickIntents.onAccountExpandClick(clickedAccount)
            }
        }
        val converter = AccountCryptoPortfolioItemStateConverter(
            appCurrency = appCurrency,
            account = account,
            onItemClick = onItemClick.takeIf { !isSearchingState },
            priceChangeLce = this.priceChangeLce,
        )
        val accountItem = converter.convert(tokenList.totalFiatBalance)
        val tokenConverter = tokenStatusConverter(this)
        val tokensListState = convertTokenList(tokenConverter, tokenList)
        val items = tokensListState.tokensList
        return TokensListPortfolioItemConverter(
            tokenItemUM = accountItem,
            isExpanded = isExpanded,
            isCollapsable = !isSearchingState,
            tokens = items.filterIsInstance<PortfolioTokensListItemUM>().toPersistentList(),
        ).convert(Unit)
    }

    private fun convertTokenList(tokenConverter: TokenItemStateConverter, tokenListParam: TokenList): TokenListUMData {
        val tokenList = if (isSearchingState) filterByQuery(tokenListParam) else tokenListParam

        return when (tokenList) {
            is TokenList.Empty -> TokenListUMData.EmptyList
            is TokenList.GroupedByNetwork -> tokenList.toGroupedItems(tokenConverter).let { grouped ->
                TokenListUMData.TokenList(
                    tokensList = grouped.toPersistentList(),
                    totalTokensCount = grouped.size,
                )
            }
            is TokenList.Ungrouped -> tokenList.toUngroupedItems(tokenConverter).let { ungrouped ->
                TokenListUMData.TokenList(
                    tokensList = ungrouped.toPersistentList(),
                    totalTokensCount = ungrouped.size,
                )
            }
        }
    }

    private fun filterByQuery(tokenList: TokenList): TokenList {
        fun List<CryptoCurrencyStatus>.filterByQuery(): List<CryptoCurrencyStatus> = filter { currency ->
            currency.currency.name.contains(searchQuery, ignoreCase = true) ||
                currency.currency.symbol.contains(searchQuery, ignoreCase = true)
        }
        return when (tokenList) {
            TokenList.Empty -> TokenList.Empty
            is TokenList.Ungrouped -> {
                val filtered = tokenList.currencies.filterByQuery()
                if (filtered.isEmpty()) TokenList.Empty else tokenList.copy(currencies = filtered)
            }
            is TokenList.GroupedByNetwork -> {
                val filteredGroups = tokenList.groups
                    .map { group ->
                        val filteredCurrencies = group.currencies.filterByQuery()
                        group.copy(currencies = filteredCurrencies)
                    }
                    .filter { group -> group.currencies.isNotEmpty() }
                if (filteredGroups.isEmpty()) TokenList.Empty else tokenList.copy(groups = filteredGroups)
            }
        }
    }
}