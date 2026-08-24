package com.tangem.domain.pay.model

import java.math.BigDecimal

data class CashbackHistory(
    val months: List<MonthlyCashback>,
) {
    data class MonthlyCashback(
        val year: Int,
        val month: Int,
        val confirmedAmount: BigDecimal,
        val currency: String,
    )
}