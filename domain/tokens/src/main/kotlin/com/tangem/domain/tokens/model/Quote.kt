package com.tangem.domain.tokens.model

import java.math.BigDecimal

data class Quote(
    val currencyId: CryptoCurrency.ID,
    val fiatRate: BigDecimal,
    val priceChange: BigDecimal,
)