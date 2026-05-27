package com.tangem.domain.markets

import kotlinx.serialization.Serializable

@Serializable
enum class PreselectedMarketsOrder(val value: String) {
    Rating("rating"),
    Trending("trending"),
    Buyers("buyers"),
    Gainers("gainers"),
    Losers("losers"),
    ;

    companion object {
        fun parse(value: String?): PreselectedMarketsOrder? = entries.firstOrNull { it.value == value }
    }
}