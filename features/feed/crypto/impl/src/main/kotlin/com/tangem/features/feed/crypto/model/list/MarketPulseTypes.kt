package com.tangem.features.feed.crypto.model.list

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.features.feed.analytics.FeedSortPeriod
import com.tangem.features.feed.analytics.FeedSortType
import com.tangem.features.feed.crypto.impl.R

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

    fun toAnalyticsPeriod(): FeedSortPeriod {
        return when (this) {
            H24 -> FeedSortPeriod.H24
            D7 -> FeedSortPeriod.D7
            M1 -> FeedSortPeriod.M1
        }
    }

    fun intervalTitle(): TextReference {
        return resourceReference(
            when (this) {
                H24 -> R.string.markets_selector_interval_24h_title
                D7 -> R.string.markets_selector_interval_7d_title
                M1 -> R.string.markets_selector_interval_1m_title
            },
        )
    }
}

/** Sorting category of the Market Pulse list — mirrors the old markets list sort types. */
internal enum class MarketPulseCategory(val id: String) {
    MarketCap(id = "market_cap"),
    TopGainers(id = "top_gainers"),
    TopLosers(id = "top_losers"),
    ExperiencedBuyers(id = "experienced_buyers"),
    ;

    fun toRequestOrder(): TokenMarketListConfig.Order {
        return when (this) {
            MarketCap -> TokenMarketListConfig.Order.ByRating
            ExperiencedBuyers -> TokenMarketListConfig.Order.Buyers
            TopGainers -> TokenMarketListConfig.Order.TopGainers
            TopLosers -> TokenMarketListConfig.Order.TopLosers
        }
    }

    /** [MarketCap] reports as [FeedSortType.Rating]: it is the same `ByRating` order the pre-tabs list sent. */
    fun toAnalyticsType(): FeedSortType {
        return when (this) {
            MarketCap -> FeedSortType.Rating
            ExperiencedBuyers -> FeedSortType.Buyers
            TopGainers -> FeedSortType.Gainers
            TopLosers -> FeedSortType.Losers
        }
    }

    fun categoryTitle(): TextReference {
        return resourceReference(
            when (this) {
                MarketCap -> R.string.markets_sort_by_rating_title
                TopGainers -> R.string.markets_sort_by_top_gainers_title
                TopLosers -> R.string.markets_sort_by_top_losers_title
                ExperiencedBuyers -> R.string.markets_sort_by_experienced_buyers_title
            },
        )
    }
}