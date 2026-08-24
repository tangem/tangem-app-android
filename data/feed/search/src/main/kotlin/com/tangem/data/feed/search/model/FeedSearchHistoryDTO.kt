package com.tangem.data.feed.search.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class FeedSearchHistoryDTO(
    val queries: List<QueryDTO> = emptyList(),
    val items: List<ItemDTO> = emptyList(),
)

@Serializable
internal data class QueryDTO(val text: String, val timestamp: Long)

@Serializable
internal sealed interface ItemDTO {

    val id: String
    val timestamp: Long

    @Serializable
    @SerialName("market_token")
    data class MarketToken(
        override val id: String,
        val name: String,
        val symbol: String,
        val imageUrl: String?,
        val currentPrice: String,
        val h24Percent: String?,
        val weekPercent: String?,
        val monthPercent: String?,
        override val timestamp: Long,
    ) : ItemDTO
}