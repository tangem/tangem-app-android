package com.tangem.domain.polymarket.model

import java.math.BigDecimal

data class PolymarketBalanceAllowance(
    val balance: BigDecimal,
    val allowances: Map<String, BigDecimal>?,
)