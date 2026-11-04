package com.tangem.features.earn.impl.widgets.calculator.model

import com.tangem.features.earn.product.api.product.EarnHorizon
import java.math.BigDecimal
import kotlin.math.sign

internal data class CalculatorInput(
    val initialFiat: BigDecimal,
    val monthlyTopUpFiat: BigDecimal,
    val horizon: EarnHorizon,
) {

    val canDecreaseInitial: Boolean
        get() = initialFiat > MIN

    val canIncreaseInitial: Boolean
        get() = initialFiat < MAX

    val canDecreaseTopUp: Boolean
        get() = monthlyTopUpFiat > MIN

    val canIncreaseTopUp: Boolean
        get() = monthlyTopUpFiat < MAX

    /** @param direction the sign of the step: `-1` or `+1` */
    fun stepInitial(direction: Int): CalculatorInput = copy(initialFiat = initialFiat.stepped(INITIAL_STEP, direction))

    /** @param direction the sign of the step: `-1` or `+1` */
    fun stepMonthlyTopUp(direction: Int): CalculatorInput =
        copy(monthlyTopUpFiat = monthlyTopUpFiat.stepped(TOP_UP_STEP, direction))

    private fun BigDecimal.stepped(step: BigDecimal, direction: Int): BigDecimal =
        (this + step * BigDecimal(direction.sign)).coerceIn(MIN, MAX)

    companion object {

        private const val DEFAULT_INITIAL = 2_500
        private const val DEFAULT_TOP_UP = 500
        private const val INITIAL_STEP_VALUE = 1_000
        private const val TOP_UP_STEP_VALUE = 100
        private const val MAX_VALUE = 1_000_000

        private val MIN = BigDecimal.ZERO
        private val MAX = BigDecimal(MAX_VALUE)
        private val INITIAL_STEP = BigDecimal(INITIAL_STEP_VALUE)
        private val TOP_UP_STEP = BigDecimal(TOP_UP_STEP_VALUE)

        val Default = CalculatorInput(
            initialFiat = BigDecimal(DEFAULT_INITIAL),
            monthlyTopUpFiat = BigDecimal(DEFAULT_TOP_UP),
            horizon = EarnHorizon.Year3,
        )
    }
}