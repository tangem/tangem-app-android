package com.tangem.data.feed.search.model

import kotlinx.serialization.Serializable

@Serializable
internal data class FeedSearchHistoryDTO(
    val queries: List<QueryDTO> = emptyList(),
    val items: List<ItemDTO> = emptyList(),
)

@Serializable
internal data class QueryDTO(val text: String, val timestamp: Long)

/**
 * Flat rather than a sealed hierarchy on purpose. A polymorphic [kind] the running version does not
 * know would fail to decode, and DataStore answers a decoding failure anywhere in the file by
 * replacing all of it with the default — so one unknown item would also delete every recent query.
 * This shape decodes an unknown [kind] without complaint; the converter drops it instead.
 */
@Serializable
internal data class ItemDTO(
    val kind: String,
    val id: String,
    val timestamp: Long,
    val marketToken: MarketTokenDTO? = null,
)

@Serializable
internal data class MarketTokenDTO(
    val name: String,
    val symbol: String,
    val imageUrl: String? = null,
    val currentPrice: String,
    val h24Percent: String? = null,
    val weekPercent: String? = null,
    val monthPercent: String? = null,
)

internal object ItemKind {

    const val MARKET_TOKEN = "market_token"
}