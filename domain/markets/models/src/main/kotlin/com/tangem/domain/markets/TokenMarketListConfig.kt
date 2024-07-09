package com.tangem.domain.markets

data class TokenMarketListConfig(
    val fiatPriceCurrency: String,
    val searchText: String?,
    val showUnder100kMarketCapTokens: Boolean,
    val priceChangeInterval: Interval,
    val order: Order,
) {

    enum class Order {
        ByRating, Trending, Buyers, TopGainers, TopLosers
    }

    enum class Interval {
        H24, WEEK, MONTH,
    }
}
