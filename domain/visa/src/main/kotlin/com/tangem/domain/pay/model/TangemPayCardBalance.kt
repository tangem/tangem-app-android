package com.tangem.domain.pay.model

import java.math.BigDecimal

data class TangemPayCardBalance(
    val fiatBalance: BigDecimal,
    val currencyCode: String,
)