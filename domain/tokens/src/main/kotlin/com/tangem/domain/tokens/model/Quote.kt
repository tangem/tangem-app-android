package com.tangem.domain.tokens.model

import java.math.BigDecimal

data class Quote(
    val tokenId: Token.ID,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
)
