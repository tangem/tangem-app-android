package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import java.math.BigDecimal

data class QuotesResponse(
    @Json(name = "quotes")
    val quotes: Map<String, Quote>,
) {

    data class Quote(
        @Json(name = "price")
        val price: BigDecimal,
        @Json(name = "priceChange24h")
        val priceChange: BigDecimal,
        @Json(name = "lastUpdatedAt")
        val lastUpdated: String,
    )
}