package com.tangem.datasource.api.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketApiKeyResponse(
    @Json(name = "apiKey") val apiKey: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "passphrase") val passphrase: String,
)