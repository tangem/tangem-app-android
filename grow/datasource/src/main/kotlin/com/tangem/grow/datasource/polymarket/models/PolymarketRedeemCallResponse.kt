package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Answer of `GET /positions/redeem/call`.
 *
 * [calls] holds the one (V1) or one to two (V2) contract calls that redeem the position, in the order to sign
 * them. [target], [value] and [data] are the pre-V2 shape of the same answer and repeat `calls[0]`; a backend
 * that predates [calls] sends only them.
 */
@JsonClass(generateAdapter = true)
data class PolymarketRedeemCallResponse(
    @Json(name = "version") val version: String? = null,
    @Json(name = "calls") val calls: List<PolymarketApprovalCallDto>? = null,
    @Json(name = "target") val target: String? = null,
    @Json(name = "value") val value: String? = null,
    @Json(name = "data") val data: String? = null,
)