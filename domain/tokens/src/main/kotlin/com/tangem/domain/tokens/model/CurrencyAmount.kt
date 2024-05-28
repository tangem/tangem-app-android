package com.tangem.domain.tokens.model

import java.math.BigDecimal

data class CurrencyAmount(
    val value: BigDecimal,
    val maxValue: BigDecimal?,
)
