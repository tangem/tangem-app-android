package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class HotCryptoResponse(
    @Json(name = "imageHost") val imageHost: String? = null,
    @Json(name = "tokens") val tokens: List<Token>,
) {

    @JsonClass(generateAdapter = true)
    data class Token(
        @Json(name = "id") val id: String,
        @Json(name = "symbol") val symbol: String,
        @Json(name = "name") val name: String,
        @Json(name = "network_id") val networkId: String,
        @Json(name = "current_price") val currentPrice: BigDecimal,
        @Json(name = "price_change_percentage_24h") val priceChangePercentage: BigDecimal,
        @Json(name = "contractAddress") val contractAddress: String? = null, // specific for token
        @Json(name = "decimal_count") val decimalCount: Int? = null, // specific for token
    )
}