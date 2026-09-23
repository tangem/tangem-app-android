package com.tangem.features.feed.rwa.categories

import com.tangem.domain.markets.CoinCategory
import java.util.Locale

/**
 * Which category the tab opens on.
 *
 * In [ReservedCategoryCodes.RESTRICTED_DEFAULT_COUNTRIES] the umbrella lists assets the user may not
 * trade, so the tab starts on commodities instead. Everywhere else it opens unfiltered, which is the
 * empty [CategorySelection] — the umbrella is substituted only at the network boundary.
 */
internal object RwaDefaultCategory {

    fun resolve(categories: List<CoinCategory>, country: String?, hasUserSelection: Boolean): CategorySelection? {
        // the categories fetch can fail, be retried from the selector, and succeed only after the user
        // has already picked — the default must not replace that pick
        if (hasUserSelection) return null

        val code = country?.uppercase(Locale.ROOT) ?: return null
        if (code !in ReservedCategoryCodes.RESTRICTED_DEFAULT_COUNTRIES) return null

        val commodities = categories.firstOrNull { it.code == ReservedCategoryCodes.COMMODITIES } ?: return null

        return CategorySelection(
            category = CategorySelection.Entry(
                id = commodities.id,
                displayName = commodities.displayName,
                code = commodities.code,
            ),
        )
    }
}