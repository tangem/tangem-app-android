package com.tangem.feature.swap.domain.models.domain

import java.math.BigDecimal

data class ExchangeQuote(
    val toAmount: BigDecimal,
    val allowanceContract: String?,
)