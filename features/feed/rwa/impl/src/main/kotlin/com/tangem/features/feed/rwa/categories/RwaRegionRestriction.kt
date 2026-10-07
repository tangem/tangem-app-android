package com.tangem.features.feed.rwa.categories

import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.messagebanner.TangemMessageBanner
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_info_20
import com.tangem.domain.markets.CoinCategory
import com.tangem.utils.CountryNames

/**
 * Whether the scope the list currently shows holds assets the user's region blocks.
 *
 * The backend evaluates `restricted` per category for the country the request carried, and never sends
 * the country list itself — so the region the notice names is the country the app asked about, not
 * anything the response spells out.
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
     * The notice for a blocked scope, or `null` when nothing is blocked.
     *
     * Names the region when the platform can name it, and falls back to the region-less wording when it
     * cannot — a bare country code reads worse than saying nothing. The restriction itself is reported
     * either way.
     *
     * @param country ISO 3166-1 alpha-2 code the restrictions were evaluated for, e.g. `US`
     */
    fun banner(
        restrictedIds: Set<String>,
        selection: CategorySelection,
        country: String?,
    ): TangemMessageBanner.State? {
        if (!isRestricted(restrictedIds = restrictedIds, selection = selection)) return null

        val regionName = CountryNames.resolveDisplayName(country)

        return TangemMessageBanner.State(
            title = resourceReference(R.string.markets_real_assets_region_restriction_title),
            description = if (regionName == null) {
                resourceReference(R.string.markets_real_assets_region_restriction_description)
            } else {
                resourceReference(
                    id = R.string.markets_real_assets_region_restriction_description_region,
                    formatArgs = wrappedList(regionName),
                )
            },
            variant = TangemMessageBanner.Variant.Warning,
            iconEnd = TangemIconUM.Icon(
                imageVector = Icons.ic_info_20,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
        )
    }
}