package com.domain.blockaid.models.transaction.simultation

import java.math.BigDecimal

data class AmountInfo(
    val amount: BigDecimal,
    val token: TokenInfo,
)