package com.tangem.grow.datasource.polymarket.socket

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("event_type")
sealed interface PolymarketMarketEventDto {

    @Serializable
    @SerialName("book")
    data class Book(
        @SerialName("asset_id") val assetId: String,
        @SerialName("bids") val bids: List<Level>,
        @SerialName("asks") val asks: List<Level>,
        @SerialName("timestamp") val timestamp: String,
    ) : PolymarketMarketEventDto

    @Serializable
    @SerialName("price_change")
    data class PriceChange(
        @SerialName("price_changes") val priceChanges: List<Change>,
        @SerialName("timestamp") val timestamp: String,
    ) : PolymarketMarketEventDto

    @Serializable
    @SerialName("best_bid_ask")
    data class BestBidAsk(
        @SerialName("asset_id") val assetId: String,
        @SerialName("best_bid") val bestBid: String,
        @SerialName("best_ask") val bestAsk: String,
        @SerialName("timestamp") val timestamp: String,
    ) : PolymarketMarketEventDto

    @Serializable
    @SerialName("last_trade_price")
    data class LastTradePrice(
        @SerialName("asset_id") val assetId: String,
        @SerialName("price") val price: String,
        @SerialName("timestamp") val timestamp: String,
    ) : PolymarketMarketEventDto

    data class Unknown(val type: String?, val payload: JsonObject) : PolymarketMarketEventDto

    @Serializable
    data class Level(
        @SerialName("price") val price: String,
        @SerialName("size") val size: String,
    )

    @Serializable
    data class Change(
        @SerialName("asset_id") val assetId: String,
        @SerialName("best_bid") val bestBid: String? = null,
        @SerialName("best_ask") val bestAsk: String? = null,
    )
}