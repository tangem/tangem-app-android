package com.tangem.features.feed.rwa.categories

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
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

    /**
     * The notice for a blocked scope, or `null` when nothing is blocked. The copy is fixed: the backend
     * returns a per-category `restricted` flag and never the countries behind it, so neither the blocked
     * categories nor the regions can be named from data.
     */
    fun banner(restrictedIds: Set<String>, selection: CategorySelection): TangemMessageBanner.State? {
        if (!isRestricted(restrictedIds = restrictedIds, selection = selection)) return null

        return TangemMessageBanner.State(
            title = resourceReference(R.string.markets_real_assets_region_restriction_title),
            description = resourceReference(R.string.markets_real_assets_region_restriction_description),
            variant = TangemMessageBanner.Variant.Warning,
            iconEnd = TangemIconUM.Icon(
                imageVector = Icons.ic_info_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        )
    }
}