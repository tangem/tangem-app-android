package com.tangem.features.feed.rwa.categories

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference

/**
 * Resolves category and sector names to localized resources, falling back to the backend `displayName`.
 *
 * The backend serves names in English only, so the app keeps its own translations keyed by the stable
 * codes. A code the maps don't know degrades to `displayName` — exactly what a category added on the
 * server before the app ships a key for it should do.
 *
 * Sectors are keyed per category because the same sector name can belong to several of them, and a
 * sector code is only unique within its own category.
 */
internal object RwaCategoryNames {

    private val CATEGORY_TITLES = mapOf(
        "bonds" to R.string.markets_real_assets_category_bonds,
        "commodities" to R.string.markets_real_assets_category_commodities,
        "etfs" to R.string.markets_real_assets_category_etfs,
        "stablecoins" to R.string.markets_real_assets_category_stablecoins,
        "xstocks" to R.string.markets_real_assets_category_xstocks,
    )

    private val SECTOR_TITLES = mapOf(
        "xstocks" to mapOf(
            "communication" to R.string.markets_real_assets_sector_xstocks_communication,
            "consumer" to R.string.markets_real_assets_sector_xstocks_consumer,
            "crypto" to R.string.markets_real_assets_sector_xstocks_crypto,
            "energy" to R.string.markets_real_assets_sector_xstocks_energy,
            "financials" to R.string.markets_real_assets_sector_xstocks_financials,
            "healthcare" to R.string.markets_real_assets_sector_xstocks_healthcare,
            "industrials" to R.string.markets_real_assets_sector_xstocks_industrials,
            "technology" to R.string.markets_real_assets_sector_xstocks_technology,
        ),
    )

    fun category(code: String, fallback: String): TextReference {
        return CATEGORY_TITLES[code]?.let(::resourceReference) ?: stringReference(fallback)
    }

    fun sector(categoryCode: String, sectorCode: String, fallback: String): TextReference {
        val titleResId = SECTOR_TITLES[categoryCode]?.get(sectorCode)

        return titleResId?.let(::resourceReference) ?: stringReference(fallback)
    }
}