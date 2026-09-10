package com.tangem.features.feed.rwa.categories

/** Category codes the app treats specially instead of offering them as plain filters. */
internal object ReservedCategoryCodes {

    /** Backs the tab's Featured rail; never offered in the Category selector. */
    const val FEATURED_ASSETS = "featured-assets"

    /** Umbrella of every real-world asset: the tab's default scope, surfaced as "All" in the selector. */
    const val REAL_WORLD_ASSETS = "real-world-assets"
}