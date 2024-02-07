package com.tangem.lib.visa.model

import org.joda.time.Instant
import java.math.BigDecimal
import java.math.BigInteger

data class VisaBalancesAndLimits(
    val balances: Balances,
    val oldLimits: Limits,
    val newLimits: Limits,
    val limitsChangeDate: Instant,
) {

    data class Balances(
        val total: BigDecimal,
        val verified: BigDecimal,
        val available: Available,
        val blocked: BigDecimal,
        val debt: BigDecimal,
        val pendingRefund: BigDecimal,
    ) {

        data class Available(
            val forPayment: BigDecimal,
            val forWithdrawal: BigDecimal,
            val forDebtPayment: BigDecimal,
        )
    }

    data class Limits(
        val spendLimit: Limit,
        val noOtpLimit: Limit,
        val singleTransactionLimit: BigDecimal,
        val expirationDate: Instant,
        val spendPeriodSeconds: BigInteger,
    ) {

        data class Limit(
            val limit: BigDecimal,
            val spent: BigDecimal,
        )
    }
}