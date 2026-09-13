package com.tangem.features.feed.crypto.model.list

import com.tangem.domain.markets.TokenMarketListConfig

/** Price-trend interval of the Market Pulse list. */
internal enum class MarketPulseInterval {
    H24,
    D7,
    M1,
    ;

    fun toBatchRequestInterval(): TokenMarketListConfig.Interval {
        return when (this) {
            H24 -> TokenMarketListConfig.Interval.H24
            D7 -> TokenMarketListConfig.Interval.WEEK
            M1 -> TokenMarketListConfig.Interval.MONTH
        }
    }
}

/** Sorting category of the Market Pulse list — mirrors the old markets list sort types. */
internal enum class MarketPulseCategory(val id: String) {
    MarketCap(id = "market_cap"),
    TopGainers(id = "top_gainers"),
    TopLosers(id = "top_losers"),
    ExperiencedBuyers(id = "experienced_buyers"),
    Trending(id = "trending"),
    ;

    fun toRequestOrder(): TokenMarketListConfig.Order {
        return when (this) {
            MarketCap -> TokenMarketListConfig.Order.ByRating
            Trending -> TokenMarketListConfig.Order.Trending
            ExperiencedBuyers -> TokenMarketListConfig.Order.Buyers
            TopGainers -> TokenMarketListConfig.Order.TopGainers
            TopLosers -> TokenMarketListConfig.Order.TopLosers
        }
    }
}