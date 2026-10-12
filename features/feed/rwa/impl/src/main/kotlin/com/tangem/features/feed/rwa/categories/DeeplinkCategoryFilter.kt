package com.tangem.features.feed.rwa.categories

import com.tangem.domain.markets.CoinCategory

/**
 * Parked until the categories the id is resolved against arrive — the tab is normally built by the very
 * deeplink carrying it. The id is spent on the first attempt whether or not it resolves.
 */
internal class DeeplinkCategoryFilter {

    private var categoryId: String? = null

    fun park(categoryId: String?) {
        this.categoryId = categoryId
    }

    fun take(categories: List<CoinCategory>): CategorySelection? {
        val parked = categoryId ?: return null
        categoryId = null

        return categories.resolveDeeplinkSelection(categoryId = parked)
    }
}