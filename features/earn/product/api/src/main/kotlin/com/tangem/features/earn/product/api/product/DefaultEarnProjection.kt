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

    private val monthlyFactor: BigDecimal = monthlyFactorOf(yearlyFactor = BigDecimal.ONE + effectiveRate)

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

    /**
     * The 12th root of [yearlyFactor], refined by Newton steps in [BigDecimal] from a [Double] estimate: a [Double]
     * root alone would leave a whole year a hair away from `1 + netRate`, which flips an exact half cent.
     */
    private fun monthlyFactorOf(yearlyFactor: BigDecimal): BigDecimal {
        val months = BigDecimal(MONTHS_IN_YEAR)
        val monthsLess = BigDecimal(MONTHS_IN_YEAR - 1)
        var factor = BigDecimal.valueOf(Math.pow(yearlyFactor.toDouble(), 1.0 / MONTHS_IN_YEAR))

        repeat(NEWTON_STEPS) {
            val power = factor.pow(MONTHS_IN_YEAR - 1, ROOT_CONTEXT)
            factor = factor.multiply(monthsLess, ROOT_CONTEXT)
                .add(yearlyFactor.divide(power, ROOT_CONTEXT), ROOT_CONTEXT)
                .divide(months, ROOT_CONTEXT)
        }
        return factor
    }

    /** Arithmetic noise is rounded off first, otherwise an exact half cent can land on either side. */
    private fun BigDecimal.toCents(): BigDecimal =
        setScale(NOISE_SCALE, RoundingMode.HALF_EVEN).setScale(CENTS_SCALE, RoundingMode.HALF_EVEN)

    private companion object {
        const val MONTHS_IN_YEAR = 12
        const val CENTS_SCALE = 2
        const val NOISE_SCALE = 20
        const val NEWTON_STEPS = 3
        const val ROOT_PRECISION = 50
        val MATH_CONTEXT: MathContext = MathContext.DECIMAL128
        val ROOT_CONTEXT: MathContext = MathContext(ROOT_PRECISION)
    }
}