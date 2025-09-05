package com.tangem.domain.onramp.utils

import java.math.BigDecimal

internal fun calculateRateDif(currentTokenRate: BigDecimal, bestRate: BigDecimal?): BigDecimal? {
    if (bestRate == null) return null
    return BigDecimal.ONE - currentTokenRate / bestRate
}