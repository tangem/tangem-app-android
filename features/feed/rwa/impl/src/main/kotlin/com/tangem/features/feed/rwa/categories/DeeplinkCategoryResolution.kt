package com.tangem.features.feed.rwa.categories

import com.tangem.domain.markets.CoinCategory

/**
 * The `category` parameter is matched against [CoinCategory.id] first and the stable [CoinCategory.code]
 * second, because either can reach the app: the id is what the selector round-trips, the code is the
 * backend contract string a campaign is more likely to be written against.
 *
 * Only categories the selector itself offers are candidates. The umbrella resolves to an empty
 * selection — it is "All", not a filter — and anything else unmatched returns `null`, leaving the tab
 * exactly as it was.
 */
internal fun List<CoinCategory>.resolveDeeplinkSelection(categoryId: String?): CategorySelection? {
    if (categoryId == null) return null
    if (categoryId == ReservedCategoryCodes.REAL_WORLD_ASSETS) return CategorySelection()

    val selectable = filter { it.isVisible && it.code != ReservedCategoryCodes.FEATURED_ASSETS }
    val match = selectable.firstOrNull { it.id == categoryId }
        ?: selectable.firstOrNull { it.code == categoryId }
        ?: return null

    return CategorySelection(
        category = CategorySelection.Entry(id = match.id, displayName = match.displayName, code = match.code),
    )
}