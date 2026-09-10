package com.tangem.domain.markets

import java.math.BigDecimal

/**
 * Market category — a group of coins with its aggregated market data
 *
 * @property description               `null` when the category has no description
 * @property marketCapChangePercent24h market cap change over the last 24 hours as a **fraction**
 *                                     (`-0.0152` for -1.52%), matching [TokenQuotesShort]
 * @property isFeatured                category is curated as featured by the backend
 * @property tokensCount               number of coins in the category
 */
data class MarketCategory(
    val id: String,
    val name: String,
    val description: String?,
    val marketCap: BigDecimal?,
    val marketCapChangePercent24h: BigDecimal?,
    val volume24h: BigDecimal?,
    val isFeatured: Boolean,
    val tokensCount: Int,
)

/**
 * One page of [MarketCategory]
 *
 * @property total number of categories on the server, not the size of this page
 */
data class MarketCategoryPage(
    val categories: List<MarketCategory>,
    val total: Int,
    val offset: Int,
    val limit: Int,
)