package com.tangem.lib.visa.model

import org.joda.time.Instant
import java.math.BigDecimal
import java.math.BigInteger

data class VisaContractInfo(
    val token: Token,
    val balances: Balances,
    val oldLimits: Limits,
    val newLimits: Limits,
    val paymentAccountAddress: String,
    val limitsChangeDate: Instant,
) {

    data class Token(
        val name: String,
        val symbol: String,
        val decimals: Int,
        val address: String,
    )

    data class Balances(
        val total: BigDecimal,
        val verified: BigDecimal,
        val available: Available,
        val blocked: BigDecimal,
        val debt: BigDecimal,
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