package com.tangem.domain.markets

import java.math.BigDecimal

data class TokenMarketListWithMaxApy(
    val tokens: List<TokenMarket>,
    val maxApy: BigDecimal?,
)