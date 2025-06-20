package com.tangem.domain.swap.models

import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.tokens.model.CryptoCurrencyStatus

data class SwapCurrencies(
    val fromGroup: SwapCurrenciesGroup,
    val toGroup: SwapCurrenciesGroup,
)

data class SwapCurrenciesGroup(
    val available: List<SwapCryptoCurrency>,
    val unavailable: List<SwapCryptoCurrency>,
    val afterSearch: Boolean,
)

data class SwapCryptoCurrency(
    val currencyStatus: CryptoCurrencyStatus,
    val providers: List<ExpressProvider>,
)