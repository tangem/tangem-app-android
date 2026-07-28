package com.tangem.domain.pay.model

import java.math.BigDecimal

/**
 * Customer cashback history from `GET /v1/customer/cashback/history`.
 *
 * Confirmed cashback grouped by calendar month. Drives the monthly earnings histogram on the
 * Cashback screen.
 */
data class CashbackHistory(
    val currency: String,
    /** Confirmed cashback per calendar month, ordered oldest to newest. */
    val months: List<MonthlyCashback>,
) {

    data class MonthlyCashback(
        val year: Int,
        /** 1-based calendar month (6 = June). */
        val month: Int,
        /** Total confirmed cashback for this month; negative for refunds. */
        val confirmedAmount: BigDecimal,
    )
}