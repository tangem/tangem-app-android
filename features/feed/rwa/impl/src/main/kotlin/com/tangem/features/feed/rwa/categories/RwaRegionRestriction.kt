package com.tangem.features.feed.rwa.categories

import com.tangem.domain.markets.CoinCategory

/**
 * Whether the scope the list currently shows holds assets the user's region blocks.
 *
 * The backend evaluates `restricted` per category for the country the request carried, and never sends
 * the country list itself — so this is the only thing the app can say about a region, and the banner copy
 * is fixed rather than assembled from data.
 */
internal object RwaRegionRestriction {

    /**
     * Ids of the categories blocked in the user's region. Only visible ones qualify: a token's own
     * restriction flag aggregates exactly those, and the invisible umbrella is blocked nowhere.
     */
    fun restrictedIds(categories: List<CoinCategory>): Set<String> = categories
        .filter { it.isVisible && it.isRestricted }
        .mapTo(destination = mutableSetOf()) { it.id }

    /**
     * Only a picked category answers. The unfiltered scope mixes blocked and allowed assets, and the
     * notice names one category — so it stays silent there, and the per-asset marks carry the warning
     * instead.
     */
    fun isRestricted(restrictedIds: Set<String>, selection: CategorySelection): Boolean {
        val picked = selection.category ?: return false

        return picked.id in restrictedIds
    }
}