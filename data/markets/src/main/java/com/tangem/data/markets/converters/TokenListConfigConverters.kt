package com.tangem.data.markets.converters

import com.tangem.domain.markets.PriceChangeInterval
import com.tangem.domain.markets.TokenMarketListConfig

fun TokenMarketListConfig.Interval.toRequestParam(): String = when (this) {
    TokenMarketListConfig.Interval.H24 -> "24h"
    TokenMarketListConfig.Interval.WEEK -> "1w"
    TokenMarketListConfig.Interval.MONTH -> "30d"
}

fun TokenMarketListConfig.Order.toRequestParam(): String = when (this) {
    TokenMarketListConfig.Order.ByRating -> "rating"
    TokenMarketListConfig.Order.Trending -> "trending"
    TokenMarketListConfig.Order.Buyers -> "buyers"
    TokenMarketListConfig.Order.TopGainers -> "gainers"
    TokenMarketListConfig.Order.TopLosers -> "losers"
}

fun PriceChangeInterval.toRequestParam(): String = when (this) {
    PriceChangeInterval.H24 -> "24h"
    PriceChangeInterval.WEEK -> "1w"
    PriceChangeInterval.MONTH -> "1m"
    PriceChangeInterval.MONTH3 -> "3m"
    PriceChangeInterval.MONTH6 -> "6m"
    PriceChangeInterval.YEAR -> "1y"
    PriceChangeInterval.ALL_TIME -> "all"
}
