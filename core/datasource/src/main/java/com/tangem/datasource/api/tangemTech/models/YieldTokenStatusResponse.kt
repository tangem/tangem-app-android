package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldTokenStatusResponse(
    @Json(name = "tokenAddress") val tokenAddress: String,
    @Json(name = "tokenSymbol") val tokenSymbol: String,
    @Json(name = "userBalance") val userBalance: String,
    @Json(name = "earnedYield") val earnedYield: String,
    @Json(name = "currentApy") val currentApy: BigDecimal,
    @Json(name = "totalDeposited") val totalDeposited: String,
    @Json(name = "moduleAddress") val moduleAddress: String,
    @Json(name = "status") val status: String,
    @Json(name = "lastUpdateAt") val lastUpdateAt: String,
)