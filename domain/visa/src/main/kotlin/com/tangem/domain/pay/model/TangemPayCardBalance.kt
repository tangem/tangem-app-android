package com.tangem.domain.pay.model

import java.math.BigDecimal

data class TangemPayCardBalance(
    val balance: BigDecimal,
    val currencyCode: String,
)