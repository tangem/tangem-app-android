package com.tangem.features.earn.impl.analytics

import com.tangem.features.earn.api.overview.EarnProductRate
import java.math.BigDecimal
import java.math.RoundingMode

private const val APY_SCALE = 2

private val PERCENT_FACTOR = BigDecimal("100")

private data class UsdBucket(val upperBound: BigDecimal, val label: String)

private val USD_BUCKETS = listOf(
    UsdBucket(upperBound = BigDecimal("50"), label = "<50"),
    UsdBucket(upperBound = BigDecimal("250"), label = "50-250"),
    UsdBucket(upperBound = BigDecimal("1000"), label = "250-1K"),
    UsdBucket(upperBound = BigDecimal("5000"), label = "1K-5K"),
    UsdBucket(upperBound = BigDecimal("25000"), label = "5K-25K"),
    UsdBucket(upperBound = BigDecimal("100000"), label = "25K-100K"),
)

private const val TOP_USD_BUCKET = ">100K"

internal fun EarnProductRate.toAnalyticsApy(): String =
    fraction.multiply(PERCENT_FACTOR).setScale(APY_SCALE, RoundingMode.HALF_UP).toPlainString()

internal fun usdBucketOf(usd: BigDecimal?): String? {
    usd ?: return null

    return USD_BUCKETS.firstOrNull { usd < it.upperBound }?.label ?: TOP_USD_BUCKET
}