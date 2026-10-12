package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketActivityPageResponse(
    @Json(name = "items") val items: List<PolymarketActivityDto>,
    @Json(name = "nextCursor") val nextCursor: String? = null,
)

@JsonClass(generateAdapter = true)
data class PolymarketActivityDto(
    @Json(name = "id") val id: String,
    @Json(name = "type") val type: String,
    @Json(name = "occurredAt") val occurredAt: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "transactionHash") val transactionHash: String? = null,
    @Json(name = "market") val market: PolymarketActivityMarketDto? = null,
    @Json(name = "order") val order: PolymarketActivityOrderDto? = null,
    @Json(name = "redeem") val redeem: PolymarketActivityRedeemDto? = null,
)

@JsonClass(generateAdapter = true)
data class PolymarketActivityMarketDto(
    @Json(name = "conditionId") val conditionId: String,
    @Json(name = "outcomeIndex") val outcomeIndex: Int,
    @Json(name = "shares") val shares: String,
    @Json(name = "outcome") val outcome: String? = null,
    @Json(name = "marketId") val marketId: String? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "negRisk") val isNegRisk: Boolean? = null,
    @Json(name = "event") val event: PolymarketActivityEventDto? = null,
)

@JsonClass(generateAdapter = true)
data class PolymarketActivityOrderDto(
    @Json(name = "assetId") val assetId: String,
    @Json(name = "averagePrice") val averagePrice: String,
    @Json(name = "platformFee") val platformFee: String,
    @Json(name = "builderFee") val builderFee: String,
    @Json(name = "notional") val notional: String,
    @Json(name = "tradeCount") val tradeCount: Int,
)

@JsonClass(generateAdapter = true)
data class PolymarketActivityRedeemDto(
    @Json(name = "result") val result: String,
)

@JsonClass(generateAdapter = true)
data class PolymarketActivityEventDto(
    @Json(name = "eventId") val eventId: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "title") val title: String,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
)