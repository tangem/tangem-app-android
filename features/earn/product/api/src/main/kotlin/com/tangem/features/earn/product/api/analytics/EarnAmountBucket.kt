package com.tangem.features.earn.product.api.analytics

import java.math.BigDecimal

object EarnAmountBucket {

    private data class Bucket(val upperBound: BigDecimal, val label: String)

    private val buckets = listOf(
        Bucket(upperBound = BigDecimal("50"), label = "<50"),
        Bucket(upperBound = BigDecimal("250"), label = "50-250"),
        Bucket(upperBound = BigDecimal("1000"), label = "250-1K"),
        Bucket(upperBound = BigDecimal("5000"), label = "1K-5K"),
        Bucket(upperBound = BigDecimal("25000"), label = "5K-25K"),
        Bucket(upperBound = BigDecimal("100000"), label = "25K-100K"),
    )

    private const val TOP_BUCKET = ">100K"

    fun of(usd: BigDecimal): String = buckets.firstOrNull { usd < it.upperBound }?.label ?: TOP_BUCKET
}