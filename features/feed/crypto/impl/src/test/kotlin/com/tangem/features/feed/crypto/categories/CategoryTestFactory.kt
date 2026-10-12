package com.tangem.features.feed.crypto.categories

import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.markets.MarketCategory
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorFooterUM
import com.tangem.features.feed.crypto.categories.ui.state.MarketCategorySelectorUM
import java.math.BigDecimal

/**
 * Fixtures for the category selector tests. Every argument is defaulted so a test overrides only the
 * fields it asserts on.
 *
 * [MarketCategory.marketCap] defaults to null on purpose: a non-null one sends the row converter into
 * `CompactDecimalFormat`, an `android.icu` class the JVM test runtime does not stub. The em-dash
 * fallback is the only subtitle these tests can observe.
 */

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

internal fun createSelectorState(
    search: TangemSearch.State = createSearchState(),
    isEdited: Boolean = false,
    footer: MarketCategorySelectorFooterUM = MarketCategorySelectorFooterUM(onReset = {}, onApply = {}),
    content: MarketCategorySelectorUM.ContentUM = MarketCategorySelectorUM.ContentUM.Loading,
): MarketCategorySelectorUM = MarketCategorySelectorUM(
    search = search,
    isEdited = isEdited,
    footer = footer,
    content = content,
)

internal fun createSearchState(query: String = "", isActive: Boolean = false): TangemSearch.State = TangemSearch.State(
    placeholderText = stringReference("Search"),
    query = query,
    onQueryChange = {},
    isActive = isActive,
    onActiveChange = {},
)