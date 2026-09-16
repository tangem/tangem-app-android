@file:Suppress("BooleanPropertyNaming")

package com.tangem.grow.datasource.polymarket.geo.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketGeoblockResponse(
    @Json(name = "blocked") val blocked: Boolean,
)