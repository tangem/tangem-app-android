package com.tangem.features.polymarket.impl.placeprediction.model

import com.tangem.features.polymarket.impl.common.PolymarketConst
import java.math.BigDecimal
import java.math.RoundingMode

internal object SharesCalculator {

    private val HUNDRED = BigDecimal(100)
    private const val WHOLE_SHARES_SCALE = 0
    private const val FULL_PERCENT = 100

    /**
     * @return the largest amount of [held] shares the market can accept, truncated onto its 2-decimal grid.
     *  This is the only amount that closes a position completely, so it is never rounded to whole shares.
     */
    fun available(held: BigDecimal): BigDecimal = held.setScale(PolymarketConst.SHARES_DECIMALS, RoundingMode.FLOOR)

    /** @return the smallest amount that can be selected out of [available] — a sale never leaves zero behind. */
    fun minimum(available: BigDecimal): BigDecimal = available.coerceAtMost(BigDecimal.ONE)

    /**
     * @return [percent] of [available] in whole shares, never below [minimum]; the full percentage returns
     *  [available] itself so the position can be closed to the last fraction.
     */
    fun fraction(available: BigDecimal, percent: Int): BigDecimal = if (percent == FULL_PERCENT) {
        available
    } else {
        available.multiply(BigDecimal(percent)).divide(HUNDRED).selectable(available = available)
    }

    fun increased(selected: BigDecimal, available: BigDecimal): BigDecimal =
        selected.add(PolymarketConst.SHARES_STEP).selectable(available = available)

    fun decreased(selected: BigDecimal, available: BigDecimal): BigDecimal =
        selected.subtract(PolymarketConst.SHARES_STEP).selectable(available = available)

    fun isSteppable(available: BigDecimal): Boolean = available > minimum(available)

    private fun BigDecimal.selectable(available: BigDecimal): BigDecimal = setScale(
        WHOLE_SHARES_SCALE,
        RoundingMode.FLOOR,
    ).coerceIn(minimum(available), available)
}