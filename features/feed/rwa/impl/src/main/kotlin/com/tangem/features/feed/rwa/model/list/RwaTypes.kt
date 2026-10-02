package com.tangem.features.feed.rwa.model.list

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.markets.TokenMarketListConfig
import com.tangem.features.feed.analytics.FeedSortPeriod
import com.tangem.features.feed.analytics.FeedSortType
import com.tangem.features.feed.rwa.categories.CategorySelection

internal enum class RwaInterval(val title: TextReference) {
    H24(title = resourceReference(R.string.markets_selector_interval_24h_title)),
    D7(title = resourceReference(R.string.markets_selector_interval_7d_title)),
    M1(title = resourceReference(R.string.markets_selector_interval_1m_title)),
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
}

internal enum class RwaSort(val id: String, val title: TextReference) {
    MarketCap(id = "market_cap", title = resourceReference(R.string.markets_real_assets_sort_market_cap)),
    TopGainers(id = "top_gainers", title = resourceReference(R.string.markets_real_assets_sort_top_gainers)),
    TopLosers(id = "top_losers", title = resourceReference(R.string.markets_real_assets_sort_top_losers)),
    ;

    fun toRequestOrder(): TokenMarketListConfig.Order {
        return when (this) {
            MarketCap -> TokenMarketListConfig.Order.ByRating
            TopGainers -> TokenMarketListConfig.Order.TopGainers
            TopLosers -> TokenMarketListConfig.Order.TopLosers
        }
    }

    /** [MarketCap] reports as [FeedSortType.Rating]: it is the same `ByRating` order the pre-tabs list sent. */
    fun toAnalyticsType(): FeedSortType {
        return when (this) {
            MarketCap -> FeedSortType.Rating
            TopGainers -> FeedSortType.Gainers
            TopLosers -> FeedSortType.Losers
        }
    }
}

/**
 * One list request. [searchText] is the search tab's only extra dimension — the feed tab leaves it null,
 * and carrying it here rather than as a `reload()` argument is what keeps the query and the rest of the
 * config from ever disagreeing.
 */
internal data class RwaListRequest(
    val interval: RwaInterval = RwaInterval.H24,
    val sort: RwaSort = RwaSort.MarketCap,
    val selection: CategorySelection = CategorySelection(),
    val searchText: String? = null,
    val country: String? = null,
)