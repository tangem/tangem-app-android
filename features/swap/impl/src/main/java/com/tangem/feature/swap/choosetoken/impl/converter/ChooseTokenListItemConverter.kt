package com.tangem.feature.swap.choosetoken.impl.converter

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.common.ui.account.TokensListPortfolioItemConverter
import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.common.ui.tokens.TokenItemGrouping.toGroupedItems
import com.tangem.common.ui.tokens.TokenItemGrouping.toUngroupedItems
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.icons.IconTint
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.feature.swap.choosetoken.api.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.feature.swap.choosetoken.impl.model.ClickIntents
import com.tangem.feature.swap.models.TokenListUMData
import com.tangem.feature.swap.presentation.R
import kotlinx.collections.immutable.toPersistentList

internal class ChooseTokenListItemConverter(
    private val appCurrency: AppCurrency,
    private val params: TokenConverterParams,
    private val clickIntents: ClickIntents,
    private val searchQuery: SearchQuery,
    private val tokenFilter: (AccountStatus.CryptoPortfolio, CryptoCurrencyStatus) -> Boolean,
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
                account = params.mainAccount,
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
        return TokenListUMData.AccountList(
            tokensList = accountItems.toPersistentList(),
            totalTokensCount = accountItems.sumOf { portfolio -> portfolio.tokensItemsList.size },
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
        val fiatAmountStateProvider: ((TotalFiatBalance) -> FiatAmountState?) = { totalBalance ->
            when {
                isSearchingState -> FiatAmountState.Empty
                !isExpanded -> FiatAmountState.Icon(R.drawable.ic_chewron_down_20, IconTint.Informative)
                else -> AccountCryptoPortfolioItemStateConverter
                    .createFiatAmountState(totalBalance, appCurrency)
            }
        }

        val converter = AccountCryptoPortfolioItemStateConverter(
            appCurrency = appCurrency,
            account = account,
            onItemClick = onItemClick.takeIf { !isSearchingState },
            priceChangeLce = this.priceChangeLce,
            fiatAmountStateProvider = fiatAmountStateProvider,
            subtitle2StateProvider = { _ -> null },
        )
        val accountItem = converter.convert(tokenList.totalFiatBalance)
        val tokenConverter = tokenStatusConverter(this)
        val tokensListState = convertTokenList(tokenConverter, tokenList, this)
        val items = tokensListState.tokensList
        return TokensListPortfolioItemConverter(
            tokenItemUM = accountItem,
            isExpanded = isExpanded,
            isCollapsable = !isSearchingState,
            tokens = items.filterIsInstance<PortfolioTokensListItemUM>().toPersistentList(),
        ).convert(Unit)
    }

    private fun convertTokenList(
        tokenConverter: TokenItemStateConverter,
        tokenListParam: TokenList,
        account: AccountStatus.CryptoPortfolio,
    ): TokenListUMData {
        return when (val tokenList = filterTokenList(tokenListParam, account)) {
            is TokenList.Empty -> TokenListUMData.EmptyList
            is TokenList.GroupedByNetwork -> TokenListUMData.TokenList(
                tokensList = tokenList.toGroupedItems(tokenConverter).toPersistentList(),
                totalTokensCount = tokenList.flattenCurrencies().size,
            )
            is TokenList.Ungrouped -> TokenListUMData.TokenList(
                tokensList = tokenList.toUngroupedItems(tokenConverter).toPersistentList(),
                totalTokensCount = tokenList.flattenCurrencies().size,
            )
        }
    }

    private fun filterTokenList(tokenList: TokenList, account: AccountStatus.CryptoPortfolio): TokenList {
        fun List<CryptoCurrencyStatus>.filterCurrencies(): List<CryptoCurrencyStatus> = filter { currency ->
            currency.filterByQuery() && tokenFilter(account, currency)
        }

        return when (tokenList) {
            TokenList.Empty -> TokenList.Empty
            is TokenList.Ungrouped -> {
                val filtered = tokenList.currencies.filterCurrencies()
                if (filtered.isEmpty()) TokenList.Empty else tokenList.copy(currencies = filtered)
            }
            is TokenList.GroupedByNetwork -> {
                val filteredGroups = tokenList.groups
                    .map { group ->
                        val filteredCurrencies = group.currencies.filterCurrencies()
                        group.copy(currencies = filteredCurrencies)
                    }
                    .filter { group -> group.currencies.isNotEmpty() }
                if (filteredGroups.isEmpty()) TokenList.Empty else tokenList.copy(groups = filteredGroups)
            }
        }
    }

    private fun CryptoCurrencyStatus.filterByQuery(): Boolean {
        if (!isSearchingState) return true
        val isSearchFilter = currency.name.contains(searchQuery.value, ignoreCase = true) ||
            currency.symbol.contains(searchQuery.value, ignoreCase = true)
        return isSearchFilter
    }
}