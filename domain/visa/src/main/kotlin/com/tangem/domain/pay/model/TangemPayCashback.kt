package com.tangem.domain.pay.model

import org.joda.time.DateTime
import java.math.BigDecimal

/**
 * @property currency accounting currency of every amount in this model — cashback is calculated in fiat
 *                    (backend: always `USD`).
 * @property payoutCurrency currency of the on-chain payout, i.e. the asset the cashback is actually paid in
 *                          (backend: `USDC`). `null` when the backend omits it, which it may do while the
 *                          cashback program status is not `enabled`.
 */
data class TangemPayCashback(
    val confirmedAmount: BigDecimal,
    val totalEarnedAmount: BigDecimal?,
    val currency: String,
    val payoutCurrency: String?,
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