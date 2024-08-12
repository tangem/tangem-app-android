package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenQuotesShort(
    val currentPrice: BigDecimal,
    val h24ChangePercent: BigDecimal,
    val weekChangePercent: BigDecimal,
    val monthChangePercent: BigDecimal,
)
