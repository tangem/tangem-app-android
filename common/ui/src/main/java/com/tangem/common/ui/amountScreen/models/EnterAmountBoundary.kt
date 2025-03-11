package com.tangem.common.ui.amountScreen.models

import java.math.BigDecimal

data class EnterAmountBoundary(
    val amount: BigDecimal? = null,
    val fiatAmount: BigDecimal? = null,
    val fiatRate: BigDecimal? = null,
) {
    constructor(
        amount: BigDecimal? = null,
        fiatRate: BigDecimal? = null,
    ) : this(
        amount = amount,
        fiatAmount = if (amount != null && fiatRate != null) {
            amount * fiatRate
        } else {
            null
        },
        fiatRate = fiatRate,
    )
}