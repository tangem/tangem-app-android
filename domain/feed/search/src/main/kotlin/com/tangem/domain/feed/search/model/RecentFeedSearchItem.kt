package com.tangem.domain.feed.search.model

import com.tangem.domain.markets.TokenMarketParams

/**
 * A search result the user has opened before, kept so the feed search screen can offer it again.
 *
 * Each variant carries the full snapshot its destination screen needs, not just an identifier:
 * reopening happens offline, straight from local history, with no chance to re-resolve the payload.
 */
sealed interface RecentFeedSearchItem {

    /** Unique within the variant — two variants may legitimately share a value. */
    val id: String

    data class MarketToken(val token: TokenMarketParams) : RecentFeedSearchItem {

        override val id: String get() = token.id.value
    }
}