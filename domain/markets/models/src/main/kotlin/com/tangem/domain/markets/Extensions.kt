package com.tangem.domain.markets

import com.tangem.domain.tokens.model.Quote

fun Quote.Value.toFull() = TokenQuotesFull(
    currentPrice = fiatRate,
    h24ChangePercent = h24ChangePercent,
    weekChangePercent = weekChangePercent,
    monthChangePercent = monthChangePercent,
    m3ChangePercent = null,
    m6ChangePercent = null,
    yearChangePercent = null,
    allTimeChangePercent = null,
)