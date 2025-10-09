package com.tangem.features.onramp.tokenlist.entity.transformer

import com.tangem.common.ui.account.AccountCryptoPortfolioItemStateConverter
import com.tangem.core.ui.components.token.state.TokenItemState
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.onramp.swap.entity.AccountAvailabilityUM
import com.tangem.features.onramp.tokenlist.entity.utils.OnrampTokenItemStateConverterFactory
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class UpdateAccountTokenItemConverter(
    private val appCurrency: AppCurrency,
    private val unavailableErrorText: TextReference,
    onItemClick: (TokenItemState, CryptoCurrencyStatus) -> Unit,
) : Converter<AccountAvailabilityUM, TokensListItemUM.Portfolio> {

    private val availableConverter = OnrampTokenItemStateConverterFactory
        .createAvailableItemConverter(appCurrency, onItemClick)

    private val unavailableConverter = OnrampTokenItemStateConverterFactory
        .createUnavailableItemConverterV2(appCurrency = appCurrency, unavailableErrorText = unavailableErrorText)

    override fun convert(value: AccountAvailabilityUM): TokensListItemUM.Portfolio {
        return TokensListItemUM.Portfolio(
            tokenItemUM = AccountCryptoPortfolioItemStateConverter(
                appCurrency = appCurrency,
                account = value.account,
                onItemClick = null,
            ).convert(TotalFiatBalance.Failed),
            isExpanded = true,
            isCollapsable = false,
            tokens = value.currencyList.asSequence().map { (isAvailable, status) ->
                if (isAvailable) {
                    availableConverter.convert(status)
                } else {
                    unavailableConverter.convert(status)
                }
            }.map(TokensListItemUM::Token).toPersistentList(),
        )
    }
}