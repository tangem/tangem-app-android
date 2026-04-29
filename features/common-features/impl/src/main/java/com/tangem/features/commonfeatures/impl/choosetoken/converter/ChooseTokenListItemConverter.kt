package com.tangem.features.commonfeatures.impl.choosetoken.converter

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.common.ui.account.TokensListPortfolioItemConverter
import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.tokens.TokenConverterParams
import com.tangem.common.ui.tokens.TokenItemGrouping.toGroupedItems
import com.tangem.common.ui.tokens.TokenItemGrouping.toUngroupedItems
import com.tangem.common.ui.tokens.TokenItemStateConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.icons.IconTint
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.token.state.TokenItemState.FiatAmountState
import com.tangem.core.ui.components.tokenlist.state.PortfolioTokensListItemUM
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.features.commonfeatures.api.choosetoken.model.TokenListUMData
import com.tangem.features.commonfeatures.impl.choosetoken.model.ClickIntents
import com.tangem.features.commonfeatures.impl.R
import kotlinx.collections.immutable.toPersistentList

internal class ChooseTokenListItemConverter(
    private val appCurrency: AppCurrency,
    private val params: TokenConverterParams,
    private val clickIntents: ClickIntents,
    private val searchQuery: SearchQuery,
    private val tokenFilter: (AccountStatus, CryptoCurrencyStatus) -> Boolean,
    private val isShowPaymentAccount: Boolean,
) {

    private val isSearchingState: Boolean get() = searchQuery.isSearchingState

    private val onTokenClick: (account: AccountStatus, currencyStatus: CryptoCurrencyStatus) -> Unit =
        { account, currencyStatus ->
            clickIntents.onTokenItemClick(account, currencyStatus)
        }

    private val onAccountItemClick: (Account, isExpanded: Boolean) -> Unit = { clickedAccount, isExpanded ->
        if (isExpanded) {
            clickIntents.onAccountCollapseClick(clickedAccount)
        } else {
            clickIntents.onAccountExpandClick(clickedAccount)
        }
    }

    private val fiatAmountStateProvider: ((TotalFiatBalance, isExpanded: Boolean) -> FiatAmountState?) =
        { totalBalance, isExpanded ->
            when {
                isSearchingState -> FiatAmountState.Empty
                !isExpanded -> FiatAmountState.Icon(R.drawable.ic_chewron_down_20, IconTint.Informative)
                else -> AccountCryptoPortfolioItemStateConverter
                    .createFiatAmountState(totalBalance, appCurrency)
            }
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
            .mapNotNull { accountStatus ->
                when (accountStatus) {
                    is AccountStatus.CryptoPortfolio -> accountStatus.toPortfolioItem(params)
                    is AccountStatus.Payment -> accountStatus.createPaymentAccountItem(params.expandedAccounts)
                }
            }
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
            onAccountItemClick(clickedAccount, isExpanded)
        }

        val converter = AccountCryptoPortfolioItemStateConverter(
            appCurrency = appCurrency,
            account = account,
            onItemClick = onItemClick.takeIf { !isSearchingState },
            priceChangeLce = this.priceChangeLce,
            fiatAmountStateProvider = { fiatBalance -> fiatAmountStateProvider(fiatBalance, isExpanded) },
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

    private fun List<CryptoCurrencyStatus>.filterCurrencies(account: AccountStatus): List<CryptoCurrencyStatus> =
        filter { currency -> currency.filterByQuery() && tokenFilter(account, currency) }

    private fun filterTokenList(tokenList: TokenList, account: AccountStatus.CryptoPortfolio): TokenList {
        return when (tokenList) {
            TokenList.Empty -> TokenList.Empty
            is TokenList.Ungrouped -> {
                val filtered = tokenList.currencies.filterCurrencies(account)
                if (filtered.isEmpty()) TokenList.Empty else tokenList.copy(currencies = filtered)
            }
            is TokenList.GroupedByNetwork -> {
                val filteredGroups = tokenList.groups
                    .map { group ->
                        val filteredCurrencies = group.currencies.filterCurrencies(account)
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

    private fun AccountStatus.Payment.createPaymentAccountItem(
        expandedAccounts: Set<AccountId>,
    ): TokensListItemUM.Portfolio? {
        if (!isShowPaymentAccount) return null
        val paymentCurrency: CryptoCurrencyStatus = when (val status = this.value) {
            is PaymentAccountStatusValue.Error,
            is PaymentAccountStatusValue.IssuingCard,
            PaymentAccountStatusValue.NotCreated,
            is PaymentAccountStatusValue.UnderReview,
            PaymentAccountStatusValue.Loading,
            PaymentAccountStatusValue.Empty,
            -> return null
            is PaymentAccountStatusValue.Loaded -> status.cryptoCurrencyStatus
        }
        val account = this.account
        val tokensCount = 1
        val isExpanded = isSearchingState || expandedAccounts.contains(account.accountId)
        val onItemClick: (TokenItemState) -> Unit = {
            onAccountItemClick(account, isExpanded)
        }
        val fiatBalance: TotalFiatBalance = this.value.totalFiatBalance
        val fiatAmountState = fiatAmountStateProvider(fiatBalance, isExpanded)
        val paymentAccountItem = TokenItemState.Content(
            id = account.accountId.value,
            iconState = CurrencyIconState.PaymentAccount(),
            titleState = TokenItemState.TitleState.Content(text = account.accountName.toUM().value),
            subtitleState = TokenItemState.SubtitleState.TextContent(
                value = pluralReference(
                    R.plurals.common_tokens_count,
                    count = tokensCount,
                    formatArgs = wrappedList(tokensCount),
                ),
                isAvailable = false,
            ),
            onItemClick = onItemClick.takeIf { !isSearchingState },
            fiatAmountState = fiatAmountState,
            subtitle2State = null,
            onItemLongClick = null,
        )
        val tokenConverter = tokenStatusConverter(this)
        val filtered = listOf(paymentCurrency)
            .filterCurrencies(this)
            .map { currency -> TokensListItemUM.Token(tokenConverter.convert(currency)) }

        return TokensListPortfolioItemConverter(
            tokenItemUM = paymentAccountItem,
            isExpanded = isExpanded,
            isCollapsable = !isSearchingState,
            tokens = filtered.toPersistentList(),
        ).convert(Unit)
    }
}