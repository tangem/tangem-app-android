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

fun TokenQuotes.populateWith(quotes: TokenQuotes): TokenQuotes {
    return TokenQuotes(
        currentPrice = quotes.currentPrice,
        h24ChangePercent = quotes.h24ChangePercent ?: this.h24ChangePercent,
        weekChangePercent = quotes.weekChangePercent ?: this.weekChangePercent,
        monthChangePercent = quotes.monthChangePercent ?: this.monthChangePercent,
        m3ChangePercent = quotes.m3ChangePercent ?: this.m3ChangePercent,
        m6ChangePercent = quotes.m6ChangePercent ?: this.m6ChangePercent,
        yearChangePercent = quotes.yearChangePercent ?: this.yearChangePercent,
        allTimeChangePercent = quotes.allTimeChangePercent ?: this.allTimeChangePercent,
    )
}