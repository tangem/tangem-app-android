package com.tangem.core.ui.components.marketprice

import java.math.BigDecimal
import java.math.RoundingMode

/** Price changing type */
enum class PriceChangeType {
    UP, DOWN, NEUTRAL,
    ;

    companion object {
        @Suppress("MagicNumber")
        fun fromBigDecimal(priceChangePercent: BigDecimal): PriceChangeType {
            return when {
                priceChangePercent < BigDecimal.ZERO -> DOWN
                priceChangePercent.setScale(4, RoundingMode.HALF_UP) > BigDecimal.ZERO -> UP
                else -> NEUTRAL
            }
        }
    }
}