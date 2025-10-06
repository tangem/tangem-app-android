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
        @Json(name = "tokenAddress") val tokenAddress: String? = null,
        @Json(name = "tokenSymbol") val tokenSymbol: String? = null,
        @Json(name = "tokenName") val tokenName: String? = null,
        @Json(name = "apy") val apy: BigDecimal,
        @Json(name = "isActive") val isActive: Boolean,
        @Json(name = "chainId") val chainId: Int? = null,
    )
}