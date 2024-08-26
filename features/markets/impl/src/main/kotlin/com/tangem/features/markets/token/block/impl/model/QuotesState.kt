package com.tangem.features.markets.token.block.impl.model

import java.math.BigDecimal

internal class QuotesState(
    val currentPrice: BigDecimal,
    val h24Percent: BigDecimal,
)