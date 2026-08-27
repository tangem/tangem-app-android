package com.tangem.datasource.api.polymarket.relayer.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketNonceResponse(
    @Json(name = "nonce") val nonce: String,
)