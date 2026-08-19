package com.tangem.domain.pay.model

import org.joda.time.DateTime
import java.math.BigDecimal

data class TangemPayCashback(
    val confirmedAmount: BigDecimal,
    val totalEarnedAmount: BigDecimal?,
    val currency: String,
    val period: Period,
    val previousPayout: PreviousPayout?,
) {
    data class Period(
        val year: Int,
        val month: Int,
        val payoutStart: DateTime?,
        val payoutEnd: DateTime?,
    )

    data class PreviousPayout(
        val endDate: DateTime,
        val amount: BigDecimal,
    )
}