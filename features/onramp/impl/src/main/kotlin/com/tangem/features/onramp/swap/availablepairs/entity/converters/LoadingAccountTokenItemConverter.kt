package com.tangem.features.onramp.swap.availablepairs.entity.converters

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.AccountStatus
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class LoadingAccountTokenItemConverter(
    private val appCurrency: AppCurrency,
) : Converter<AccountStatus.Crypto.Portfolio, TokensListItemUM.Portfolio> {

    override fun convert(value: AccountStatus.Crypto.Portfolio): TokensListItemUM.Portfolio {
        val (account, currencies) = value

        return TokensListItemUM.Portfolio(
            tokenItemUM = AccountCryptoPortfolioItemStateConverter(
                appCurrency = appCurrency,
                account = account,
                onItemClick = null,
            ).convert(TotalFiatBalance.Failed),
            isExpanded = true,
            isCollapsable = false,
            tokens = currencies.flattenCurrencies()
                .map { LoadingTokenListItemConverter.convert(it.currency) }
                .toPersistentList(),
        )
    }
}