package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketPositionsResponse(
    @Json(name = "positions") val positions: List<PolymarketOpenPositionDto>,
    @Json(name = "hasNext") val hasNext: Boolean,
)

@JsonClass(generateAdapter = true)
data class PolymarketClosedPositionsResponse(
    @Json(name = "positions") val positions: List<PolymarketClosedPositionDto>,
    @Json(name = "hasNext") val hasNext: Boolean,
)

@JsonClass(generateAdapter = true)
data class PolymarketPositionResponse(@Json(name = "position") val position: PolymarketOpenPositionDto?)

@JsonClass(generateAdapter = true)
data class PolymarketOpenPositionDto(
    @Json(name = "conditionId") val conditionId: String,
    @Json(name = "assetId") val assetId: String,
    @Json(name = "title") val title: String,
    @Json(name = "outcome") val outcome: String,
    @Json(name = "size") val size: String,
    @Json(name = "avgPrice") val avgPrice: String,
    @Json(name = "curPrice") val curPrice: String,
    @Json(name = "initialValue") val initialValue: String,
    @Json(name = "currentValue") val currentValue: String,
    @Json(name = "cashPnl") val cashPnl: String,
    @Json(name = "percentPnl") val percentPnl: String,
    @Json(name = "realizedPnl") val realizedPnl: String,
    @Json(name = "totalBought") val totalBought: String,
    @Json(name = "redeemable") val isRedeemable: Boolean,
    @Json(name = "negRisk") val isNegRisk: Boolean,
    @Json(name = "acceptingOrders") val isAcceptingOrders: Boolean,
    @Json(name = "live") val isLive: Boolean,
    @Json(name = "resolutionStatus") val resolutionStatus: String,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "marketId") val marketId: String? = null,
    @Json(name = "eventId") val eventId: String? = null,
    @Json(name = "eventTitle") val eventTitle: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "outcomeIndex") val outcomeIndex: Int? = null,
)

@JsonClass(generateAdapter = true)
data class PolymarketClosedPositionDto(
    @Json(name = "conditionId") val conditionId: String,
    @Json(name = "assetId") val assetId: String,
    @Json(name = "title") val title: String,
    @Json(name = "outcome") val outcome: String,
    @Json(name = "avgPrice") val avgPrice: String,
    @Json(name = "curPrice") val curPrice: String,
    @Json(name = "totalBought") val totalBought: String,
    @Json(name = "realizedPnl") val realizedPnl: String,
    @Json(name = "closedAt") val closedAt: String,
    @Json(name = "status") val status: String,
    @Json(name = "icon") val icon: String? = null,
    @Json(name = "endDate") val endDate: String? = null,
    @Json(name = "outcomeIndex") val outcomeIndex: Int? = null,
)