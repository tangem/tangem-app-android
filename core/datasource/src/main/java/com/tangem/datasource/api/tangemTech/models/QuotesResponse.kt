package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class QuotesResponse(
    @Json(name = "quotes")
    val quotes: Map<String, Quote>,
) {

    @JsonClass(generateAdapter = true)
    data class Quote(
        @Json(name = "price")
        val price: BigDecimal?,
        @Json(name = "priceChange24h")
        val priceChange24h: BigDecimal?,
        @Json(name = "priceChange1w")
        val priceChange1w: BigDecimal?,
        @Json(name = "priceChange30d")
        val priceChange30d: BigDecimal?,
    )
}