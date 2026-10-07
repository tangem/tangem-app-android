package com.tangem.features.feed.rwa.categories

import com.tangem.domain.markets.CoinCategory
import java.util.Locale

/**
 * Which category the tab opens on.
 *
 * In [ReservedCategoryCodes.RESTRICTED_DEFAULT_COUNTRIES] the umbrella lists assets the user may not
 * trade, so the tab starts on commodities instead — or, when the backend stops serving that category or
 * blocks it too, on the first allowed one it does offer, falling back to a blocked category only when
 * every category is blocked. Everywhere else it opens unfiltered, which is the empty
 * [CategorySelection] — the umbrella is substituted only at the network boundary.
 */
internal object RwaDefaultCategory {

    fun resolve(categories: List<CoinCategory>, country: String?, hasUserSelection: Boolean): CategorySelection? {
        // the categories fetch can fail, be retried from the selector, and succeed only after the user
        // has already picked — the default must not replace that pick
        if (hasUserSelection) return null

        val code = country?.uppercase(Locale.ROOT) ?: return null
        if (code !in ReservedCategoryCodes.RESTRICTED_DEFAULT_COUNTRIES) return null

        val default = categories
            .firstOrNull { it.isSelectable() && !it.isRestricted && it.code == ReservedCategoryCodes.COMMODITIES }
            ?: categories.filter { it.isSelectable() }
                // opening on a blocked scope is the thing this default exists to avoid, so a category the
                // region allows wins over a lower-ordered one it does not
                .minWithOrNull(compareBy({ it.isRestricted }, { it.displayOrder }))
            ?: return null

        return CategorySelection(
            category = CategorySelection.Entry(
                id = default.id,
                displayName = default.displayName,
                code = default.code,
            ),
        )
    }

    /**
     * The fallback must not land on a category the selector does not offer — and least of all on the
     * umbrella, which is the very scope the restricted countries are steered away from.
     */
    private fun CoinCategory.isSelectable(): Boolean {
        return isVisible &&
            code != ReservedCategoryCodes.FEATURED_ASSETS &&
            code != ReservedCategoryCodes.REAL_WORLD_ASSETS
    }
}