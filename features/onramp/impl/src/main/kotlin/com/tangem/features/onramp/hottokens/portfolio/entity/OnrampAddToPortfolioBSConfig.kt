package com.tangem.features.onramp.hottokens.portfolio.entity

import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.domain.models.currency.CryptoCurrency

internal sealed class OnrampAddToPortfolioBSConfig {

    data class AddToPortfolio(
        val cryptoCurrency: CryptoCurrency,
        val currencyIconState: CurrencyIconState,
        val onSuccessAdding: (CryptoCurrency.ID) -> Unit,
    ) : OnrampAddToPortfolioBSConfig()
}