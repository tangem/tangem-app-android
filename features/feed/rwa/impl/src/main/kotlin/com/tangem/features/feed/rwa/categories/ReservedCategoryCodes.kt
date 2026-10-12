package com.tangem.features.feed.rwa.categories

/** Category codes the app treats specially instead of offering them as plain filters. */
internal object ReservedCategoryCodes {

    /** Backs the tab's Featured rail; never offered in the Category selector. */
    const val FEATURED_ASSETS = "featured-assets"

    /** Umbrella of every real-world asset: the tab's default scope, surfaced as "All" in the selector. */
    const val REAL_WORLD_ASSETS = "real-world-assets"

    /** Pre-applied instead of the umbrella in [RESTRICTED_DEFAULT_COUNTRIES]. */
    const val COMMODITIES = "commodities"

    /**
     * Countries where the umbrella opens onto assets the user may not trade, so the tab starts on
     * [COMMODITIES] instead. The umbrella itself is restricted nowhere, so this cannot be derived from the
     * `restricted` flag and is a product decision (TWI-1477).
     */
    val RESTRICTED_DEFAULT_COUNTRIES = setOf("US", "GB")
}