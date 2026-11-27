package com.tangem.grow.datasource.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketPriceRequest(
    @Json(name = "token_id") val tokenId: String,
    @Json(name = "side") val side: String,
)