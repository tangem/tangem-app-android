package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenQuotesShort(
    val currentPrice: BigDecimal,
    val h24ChangePercent: BigDecimal,
    val weekChangePercent: BigDecimal,
    val monthChangePercent: BigDecimal,
)

fun TokenQuotesShort.toFull() = TokenQuotes(
    currentPrice = currentPrice,
    h24ChangePercent = h24ChangePercent,
    weekChangePercent = weekChangePercent,
    monthChangePercent = monthChangePercent,
    m3ChangePercent = null,
    m6ChangePercent = null,
    yearChangePercent = null,
    allTimeChangePercent = null,
)