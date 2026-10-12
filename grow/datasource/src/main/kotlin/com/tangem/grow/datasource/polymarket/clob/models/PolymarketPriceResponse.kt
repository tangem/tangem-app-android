package com.tangem.grow.datasource.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketPriceResponse(
    @Json(name = "price") val price: String,
)