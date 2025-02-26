package com.tangem.domain.visa.model

import com.tangem.domain.appcurrency.model.AppCurrency
import org.joda.time.DateTime
import java.math.BigDecimal

data class VisaCurrency(
    val networkName: String,
    val symbol: String,
    val decimals: Int,
    val fiatRate: BigDecimal?,
    val fiatCurrency: AppCurrency,
    val balances: Balances,
    val limits: Limits,
) {

    data class Balances(
        val total: BigDecimal,
        val verified: BigDecimal,
        val available: BigDecimal,
        val blocked: BigDecimal,
        val debt: BigDecimal,
    )

    data class Limits(
        val remainingOtp: BigDecimal,
        val remainingNoOtp: BigDecimal,
        val singleTransaction: BigDecimal,
        val expirationDate: DateTime,
    )
}