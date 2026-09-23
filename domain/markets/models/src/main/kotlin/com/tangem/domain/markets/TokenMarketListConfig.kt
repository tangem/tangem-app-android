package com.tangem.domain.markets

data class TokenMarketListConfig(
    val fiatPriceCurrency: String,
    val searchText: String?,
    val priceChangeInterval: Interval,
    val order: Order,
    val shouldNetworks: Boolean? = null,
    val categoryId: String? = null,
    val sectorId: String? = null,
    val geckoCategoryId: String? = null,
    /** Country the token restriction flags are evaluated for; `null` leaves every token unrestricted. */
    val country: String? = null,
) {

    enum class Order {
        ByRating,
        Trending,
        Buyers,
        TopGainers,
        TopLosers,

        /**
         * The order curated on the backend for [categoryId] — not a user-facing sort, and meaningless
         * without a category.
         */
        ByCategory,
    }

    enum class Interval {
        H24, WEEK, MONTH,
    }
}