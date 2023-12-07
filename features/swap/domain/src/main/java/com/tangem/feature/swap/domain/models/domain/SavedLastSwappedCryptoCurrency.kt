package com.tangem.feature.swap.domain.models.domain

data class SavedLastSwappedCryptoCurrency(
    val userWalletId: String,
    val cryptoCurrencyId: String,
)