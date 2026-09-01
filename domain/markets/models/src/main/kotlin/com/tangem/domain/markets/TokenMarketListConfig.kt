package com.tangem.domain.markets

data class TokenMarketListConfig(
    val fiatPriceCurrency: String,
    val searchText: String?,
    val priceChangeInterval: Interval,
    val order: Order,
    val shouldNetworks: Boolean? = null,
    val categoryId: String? = null,
    val sectorId: String? = null,
) {

    enum class Order {
        ByRating, Trending, Buyers, TopGainers, TopLosers
    }

    enum class Interval {
        H24, WEEK, MONTH,
    }
}