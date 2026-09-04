package com.tangem.features.feed.crypto.categories

import com.tangem.domain.markets.MarketCategory
import java.math.BigDecimal

/**
 * Fixtures for the category selector tests. Every argument is defaulted so a test overrides only the
 * fields it asserts on.
 *
 * [MarketCategory.marketCap] defaults to null on purpose: a non-null one sends the row converter into
 * `CompactDecimalFormat`, an `android.icu` class the JVM test runtime does not stub. The em-dash
 * fallback is the only subtitle these tests can observe.
 */

internal const val ALL_ROW_ID = "__all__"

internal fun createCategory(
    id: String,
    name: String,
    description: String? = null,
    marketCap: BigDecimal? = null,
    marketCapChangePercent24h: BigDecimal? = null,
    volume24h: BigDecimal? = null,
    isFeatured: Boolean = false,
    tokensCount: Int = 212,
): MarketCategory = MarketCategory(
    id = id,
    name = name,
    description = description,
    marketCap = marketCap,
    marketCapChangePercent24h = marketCapChangePercent24h,
    volume24h = volume24h,
    isFeatured = isFeatured,
    tokensCount = tokensCount,
)

internal fun selectionOf(category: MarketCategory): MarketCategorySelection = MarketCategorySelection(
    category = MarketCategorySelection.Entry(id = category.id, name = category.name),
)