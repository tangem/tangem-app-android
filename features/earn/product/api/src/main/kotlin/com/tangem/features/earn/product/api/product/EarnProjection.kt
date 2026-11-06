package com.tangem.features.earn.product.api.product

import java.math.BigDecimal

interface EarnProjection {

    val netRate: BigDecimal

    fun project(initialFiat: BigDecimal, monthlyTopUpFiat: BigDecimal, horizon: EarnHorizon): EarnProjectionResult

    fun yearlyEstimate(amountFiat: BigDecimal): BigDecimal
}

@Suppress("MagicNumber")
enum class EarnHorizon(val months: Int) {
    Months6(6),
    Year1(12),
    Year3(36),
    Year5(60),
    Year10(120),
}

data class EarnProjectionResult(
    val total: BigDecimal,
    val earned: BigDecimal,
    val points: List<EarnProjectionPoint>,
)

data class EarnProjectionPoint(
    val index: Int,
    val capital: BigDecimal,
    val income: BigDecimal,
)