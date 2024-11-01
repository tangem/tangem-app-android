package com.tangem.common.ui.amountScreen.models

import java.math.BigDecimal

data class MaxEnterAmount(
    val amount: BigDecimal? = null,
    val fiatAmount: BigDecimal? = null,
    val fiatRate: BigDecimal? = null,
)
