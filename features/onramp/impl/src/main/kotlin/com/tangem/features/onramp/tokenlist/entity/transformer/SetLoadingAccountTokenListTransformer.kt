package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.filterCryptoPortfolio
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.swap.availablepairs.entity.converters.LoadingAccountTokenItemConverter
import com.tangem.features.onramp.swap.availablepairs.entity.converters.LoadingTokenListItemConverter
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMData
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

internal class SetLoadingAccountTokenListTransformer(
    appCurrency: AppCurrency,
    private val accountList: List<AccountStatus>,
    private val isAccountsMode: Boolean,
) : TokenListUMTransformer {

    private val accountListItemConverter = LoadingAccountTokenItemConverter(appCurrency)

    override fun transform(prevState: TokenListUM): TokenListUM {
        val totalTokensCount = accountList
            .filterCryptoPortfolio()
            .sumOf { account -> account.tokenList.flattenCurrencies().size }

        return prevState.copy(
            availableItems = persistentListOf(),
            unavailableItems = persistentListOf(),
            tokensListData = if (isAccountsMode) {
                TokenListUMData.AccountList(
                    tokensList = accountListItemConverter.convertList(
                        accountList.filterCryptoPortfolio(),
                    ).toPersistentList(),
                    totalTokensCount = totalTokensCount,
                )
            } else {
                TokenListUMData.TokenList(
                    tokensList = accountList.filterCryptoPortfolio().flatMap { account ->
                        LoadingTokenListItemConverter.convertList(
                            account.tokenList.flattenCurrencies().map(CryptoCurrencyStatus::currency),
                        )
                    }.toPersistentList(),
                    totalTokensCount = totalTokensCount,
                )
            },
            warning = null,
        )
    }
}