package com.tangem.core.ui.components.marketprice.utils

import com.tangem.core.ui.components.marketprice.PriceChangeType
import java.math.BigDecimal
import java.math.RoundingMode

object PriceChangeConverter {

    fun fromBigDecimal(value: BigDecimal?, scale: Int = 2): PriceChangeType {
        val formattedValue = value
            ?.movePointRight(2)
            ?.setScale(scale, RoundingMode.HALF_UP)
            ?: return PriceChangeType.NEUTRAL

        return when (formattedValue.signum()) {
            1 -> PriceChangeType.UP
            -1 -> PriceChangeType.DOWN
            else -> PriceChangeType.NEUTRAL
        }
    }
}