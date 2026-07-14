package com.tangem.domain.pay.model

import org.joda.time.DateTime
import java.math.BigDecimal

data class TangemPayCashback(
    val confirmedAmount: BigDecimal,
    val pendingAmount: BigDecimal,
    val currency: String,
    val payoutCurrency: String,
    val payoutNetwork: String,
    val period: Period,
) {
    data class Period(
        val year: Int,
        /** 1-based calendar month (6 = June). */
        val month: Int,
        val payoutStart: DateTime,
        val payoutEnd: DateTime,
    )
}