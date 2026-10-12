package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.Serializable

@JsonClass(generateAdapter = true)
data class QuotesResponse(
    @Json(name = "quotes")
    val quotes: Map<String, Quote>,
) {

    @Serializable
    @JsonClass(generateAdapter = true)
    data class Quote(
        @Json(name = "price")
        val price: SerializedBigDecimal?,
        @Json(name = "priceChange24h")
        val priceChange24h: SerializedBigDecimal?,
        @Json(name = "priceChange1w")
        val priceChange1w: SerializedBigDecimal?,
        @Json(name = "priceChange30d")
        val priceChange30d: SerializedBigDecimal?,
        @Json(name = "priceUsd")
        val priceUsd: SerializedBigDecimal?,
    ) {

        companion object {

            val EMPTY = Quote(
                price = null,
                priceChange24h = null,
                priceChange1w = null,
                priceChange30d = null,
                priceUsd = null,
            )
        }
    }
}