package com.tangem.feature.swap.domain.models.domain

data class ExchangeQuote(
    val toAmount: String,
    val allowanceContract: String?,
)