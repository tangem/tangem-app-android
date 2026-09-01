package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketConfigurationResponse(
    @Json(name = "builderCode") val builderCode: String,
)