package com.tangem.datasource.api.markets.models.response

import com.squareup.moshi.Json
import java.math.BigDecimal

data class TokenMarketListResponse(
    @Json(name = "imageHost")
    val imageHost: String?,
    @Json(name = "tokens")
    val tokens: List<Token>,
    @Json(name = "total")
    val total: Int,
    @Json(name = "limit")
    val limit: Int,
    @Json(name = "offset")
    val offset: Int,
) {
    data class Token(
        @Json(name = "id")
        val id: String,
        @Json(name = "name")
        val name: String,
        @Json(name = "symbol")
        val symbol: String,
        @Json(name = "current_price")
        val currentPrice: BigDecimal,
        @Json(name = "price_change_percentage")
        val priceChangePercentage: PriceChangePercentage,
        @Json(name = "market_rating")
        val marketRating: Int?,
        @Json(name = "market_cap")
        val marketCap: BigDecimal?,
    ) {
        data class PriceChangePercentage(
            @Json(name = "24h")
            val h24: BigDecimal,
            @Json(name = "1w")
            val week1: BigDecimal,
            @Json(name = "30d")
            val day30: BigDecimal,
        )
    }
}
