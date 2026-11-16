package com.tangem.features.earn.lending.impl.analytics

import java.math.BigDecimal

/** USD ranges the analytics groups transaction amounts into, so that exact amounts are never reported */
internal object LendingAmountBucket {

    private val upperBounds = listOf(
        BigDecimal(50) to "<50",
        BigDecimal(250) to "50-250",
        BigDecimal(1_000) to "250-1K",
        BigDecimal(5_000) to "1K-5K",
        BigDecimal(25_000) to "5K-25K",
        BigDecimal(100_000) to "25K-100K",
    )

    private const val ABOVE_ALL = ">100K"

    fun of(usdAmount: BigDecimal): String = upperBounds.firstOrNull { (bound, _) -> usdAmount < bound }?.second
        ?: ABOVE_ALL
}