package com.tangem.datasource.api.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Body of `POST /api/predictions/v1/orders/quote` (BFF `OrderQuoteRequest`).
 *
 * @property amount BUY: collateral to spend. SELL: shares to sell. Sent as a string, as the BFF expects.
 * @property slippage tolerance in percent; omitted to let the BFF apply its default.
 */
@JsonClass(generateAdapter = true)
data class PolymarketOrderQuoteRequest(
    @Json(name = "marketId") val marketId: String,
    @Json(name = "assetId") val assetId: String,
    @Json(name = "side") val side: String,
    @Json(name = "amount") val amount: String,
    @Json(name = "slippage") val slippage: String?,
)

/**
 * Response of `POST /api/predictions/v1/orders/quote` (BFF `OrderQuoteResponse`).
 *
 * Every amount arrives as a decimal string, large ones in exponential notation, so none of them are
 * declared numeric here — they are parsed into `BigDecimal` by the converter.
 */
@JsonClass(generateAdapter = true)
data class PolymarketOrderQuoteResponse(
    @Json(name = "status") val status: String,
    @Json(name = "side") val side: String,
    @Json(name = "shares") val shares: String,
    @Json(name = "notional") val notional: String,
    @Json(name = "expectedExecutionAmount") val expectedExecutionAmount: String,
    @Json(name = "averagePrice") val averagePrice: String,
    @Json(name = "worstCasePrice") val worstCasePrice: String,
    @Json(name = "fees") val fees: PolymarketOrderQuoteFeesResponse,
    @Json(name = "total") val total: String,
    @Json(name = "builderCode") val builderCode: String,
    @Json(name = "minOrderSize") val minOrderSize: String,
    @Json(name = "tickSize") val tickSize: String,
    @Json(name = "live") val isLive: Boolean,
)

/** Fee breakdown of an order quote (BFF `OrderQuoteFees`). */
@JsonClass(generateAdapter = true)
data class PolymarketOrderQuoteFeesResponse(
    @Json(name = "market") val market: String,
    @Json(name = "builder") val builder: String,
    @Json(name = "total") val total: String,
)