package com.tangem.domain.markets

/**
 * Request params for one market category listing
 *
 * Held constant for the whole listing: every page of a [MarketCategoryPage] sequence is fetched with the
 * same config, so [timestamp] pins all pages to one snapshot without any per-page bookkeeping.
 *
 * @property timestamp  snapshot the listing is pinned to; `null` lets the backend serve the latest data
 * @property searchText narrows the listing to categories matching the query; `null` lists them all
 */
data class MarketCategoryListConfig(
    val fiatCurrencySymbol: String,
    val featuredFilter: FeaturedFilter = FeaturedFilter.All,
    val timestamp: Long? = null,
    val searchText: String? = null,
) {

    /** Which side of the backend's `featured` curation a listing covers */
    enum class FeaturedFilter(val isFeatured: Boolean?) {
        All(null),
        OnlyFeatured(true),
        WithoutFeatured(false),
    }
}