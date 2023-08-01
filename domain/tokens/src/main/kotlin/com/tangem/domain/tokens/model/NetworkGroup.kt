package com.tangem.domain.tokens.model

data class NetworkGroup(
    val network: Network,
    val currencies: Set<CryptoCurrencyStatus>,
)