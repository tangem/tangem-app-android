package com.tangem.domain.tokens.model

import com.tangem.domain.models.currency.CryptoCurrency

data class CryptoCurrencyAddress(
    val cryptoCurrency: CryptoCurrency,
    val address: String,
)