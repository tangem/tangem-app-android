package com.tangem.domain.models

import java.math.BigDecimal

data class TangemPayReissueCardFee(
    val amount: BigDecimal,
    val currencyCode: String,
)