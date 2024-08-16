package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenQuotes(
    val currentPrice: BigDecimal,
    val h24ChangePercent: BigDecimal?,
    val weekChangePercent: BigDecimal?,
    val monthChangePercent: BigDecimal?,
    val m3ChangePercent: BigDecimal?,
    val m6ChangePercent: BigDecimal?,
    val yearChangePercent: BigDecimal?,
    val allTimeChangePercent: BigDecimal?,
)