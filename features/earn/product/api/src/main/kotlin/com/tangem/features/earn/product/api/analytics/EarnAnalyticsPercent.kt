package com.tangem.features.earn.product.api.analytics

import com.tangem.features.earn.api.overview.EarnProductRate
import java.math.BigDecimal
import java.math.RoundingMode

object EarnAnalyticsPercent {

    private const val SCALE = 2

    private val factor = BigDecimal("100")

    fun of(fraction: BigDecimal): String =
        fraction.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP).toPlainString()
}

fun EarnProductRate.toAnalyticsApy(): String = EarnAnalyticsPercent.of(fraction)