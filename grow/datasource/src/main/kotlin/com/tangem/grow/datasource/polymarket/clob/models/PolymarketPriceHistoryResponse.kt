package com.tangem.grow.datasource.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketPriceHistoryResponse(
    @Json(name = "history") val history: List<Point>,
) {
    @JsonClass(generateAdapter = true)
    data class Point(
        @Json(name = "t") val timestampSeconds: Long? = null,
        @Json(name = "p") val price: String? = null,
    )
}