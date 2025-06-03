package com.tangem.feature.swap.models

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup

data class CurrenciesGroupWithFromCurrency(
    val group: CurrenciesGroup,
    val fromCurrency: CryptoCurrency,
)