package com.tangem.feature.swap.models

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.feature.swap.domain.models.ui.CurrenciesGroup

data class CurrenciesGroupWithFromCurrency(
    val group: CurrenciesGroup,
    val fromCurrency: CryptoCurrency,
)