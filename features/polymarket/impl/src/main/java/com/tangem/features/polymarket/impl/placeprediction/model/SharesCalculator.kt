package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.features.polymarket.impl.common.PolymarketConst
import java.math.BigDecimal
import java.math.RoundingMode

internal object SharesCalculator {

    private val HUNDRED = BigDecimal(100)

    /**
     * @return the largest amount of [held] shares the market can accept, truncated onto its 2-decimal grid.
     */
    fun available(held: BigDecimal): BigDecimal = held.setScale(PolymarketConst.SHARES_DECIMALS, RoundingMode.FLOOR)

    /**
     * @return [percent] of [available], truncated onto the same grid and never above [available].
     */
    fun fraction(available: BigDecimal, percent: Int): BigDecimal = available
        .multiply(BigDecimal(percent))
        .divide(HUNDRED)
        .setScale(PolymarketConst.SHARES_DECIMALS, RoundingMode.FLOOR)
        .coerceIn(BigDecimal.ZERO, available)

    fun increased(selected: BigDecimal, available: BigDecimal): BigDecimal =
        selected.add(PolymarketConst.SHARES_STEP).coerceAtMost(available).grid()

    fun decreased(selected: BigDecimal): BigDecimal =
        selected.subtract(PolymarketConst.SHARES_STEP).coerceAtLeast(BigDecimal.ZERO).grid()

    fun isSteppable(available: BigDecimal): Boolean = available >= PolymarketConst.SHARES_STEP

    private fun BigDecimal.grid(): BigDecimal = setScale(PolymarketConst.SHARES_DECIMALS, RoundingMode.FLOOR)
}