package com.tangem.features.earn.product.api.product

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * The calculator rule of the PRD: the balance compounds monthly at `(1 + netRate)^(1/12)` and the top-up is added at
 * the end of each month. A negative [netRate] counts as zero, so the projection never fails.
 */
class DefaultEarnProjection(override val netRate: BigDecimal) : EarnProjection {

    private val effectiveRate: BigDecimal = netRate.max(BigDecimal.ZERO)

    private val monthlyFactor: BigDecimal =
        BigDecimal.valueOf(Math.pow(1.0 + effectiveRate.toDouble(), 1.0 / MONTHS_IN_YEAR))

    override fun project(
        initialFiat: BigDecimal,
        monthlyTopUpFiat: BigDecimal,
        horizon: EarnHorizon,
    ): EarnProjectionResult {
        var capital = initialFiat
        var invested = initialFiat

        val points = (1..horizon.months).map { month ->
            capital = capital.multiply(monthlyFactor, MATH_CONTEXT).add(monthlyTopUpFiat)
            invested = invested.add(monthlyTopUpFiat)

            val roundedCapital = capital.toCents()
            EarnProjectionPoint(index = month, capital = roundedCapital, income = (roundedCapital - invested).toCents())
        }
        val last = points.last()

        return EarnProjectionResult(total = last.capital, earned = last.income, points = points)
    }

    override fun yearlyEstimate(amountFiat: BigDecimal): BigDecimal = amountFiat.multiply(effectiveRate).toCents()

    private fun BigDecimal.toCents(): BigDecimal = setScale(CENTS_SCALE, RoundingMode.HALF_EVEN)

    private companion object {
        const val MONTHS_IN_YEAR = 12
        const val CENTS_SCALE = 2
        val MATH_CONTEXT: MathContext = MathContext.DECIMAL128
    }
}