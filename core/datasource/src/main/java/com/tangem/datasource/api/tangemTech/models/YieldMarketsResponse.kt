package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldMarketsResponse(
    @Json(name = "tokens") val marketDtos: List<MarketDto>,
    @Json(name = "lastUpdatedAt") val lastUpdated: String,
) {

    @JsonClass(generateAdapter = true)
    data class MarketDto(
        @Json(name = "tokenAddress") val tokenAddress: String,
        @Json(name = "tokenSymbol") val tokenSymbol: String,
        @Json(name = "tokenName") val tokenName: String,
        @Json(name = "apy") val apy: BigDecimal,
        @Json(name = "totalSupplied") val totalSupplied: String,
        @Json(name = "totalBorrowed") val totalBorrowed: String,
        @Json(name = "liquidityRate") val liquidityRate: String,
        @Json(name = "borrowRate") val borrowRate: String,
        @Json(name = "utilizationRate") val utilizationRate: BigDecimal,
        @Json(name = "isActive") val isActive: Boolean,
        @Json(name = "ltv") val ltv: BigDecimal,
        @Json(name = "liquidationThreshold") val liquidationThreshold: BigDecimal,
        @Json(name = "decimals") val decimals: Int,
    )
}