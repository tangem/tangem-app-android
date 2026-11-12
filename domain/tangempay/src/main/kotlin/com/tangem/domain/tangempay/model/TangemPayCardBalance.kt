package com.tangem.domain.tangempay.model

import java.math.BigDecimal

data class TangemPayCardBalance(
    val fiatBalance: BigDecimal,
    val currencyCode: String,
)