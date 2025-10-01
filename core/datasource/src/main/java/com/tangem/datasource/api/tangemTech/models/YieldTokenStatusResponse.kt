package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.math.BigDecimal

@JsonClass(generateAdapter = true)
data class YieldTokenStatusResponse(
    @Json(name = "tokenAddress") val tokenAddress: String,
    @Json(name = "tokenSymbol") val tokenSymbol: String,
    @Json(name = "tokenName") val tokenName: String,
    @Json(name = "apy") val apy: BigDecimal,
    @Json(name = "totalSupplied") val totalSupplied: String,
    @Json(name = "totalBorrowed") val totalBorrowed: String,
    @Json(name = "liquidityRate") val liquidityRate: String,
    @Json(name = "borrowRate") val borrowRate: String,
    @Json(name = "utilizationRate") val utilizationRate: Double,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "ltv") val ltv: Int,
    @Json(name = "liquidationThreshold") val liquidationThreshold: Int,
    @Json(name = "decimals") val decimals: Int,
    @Json(name = "chainId") val chainId: Int,
    @Json(name = "priority") val priority: Int,
    @Json(name = "isEnabled") val isEnabled: Boolean,
    @Json(name = "lastUpdatedAt") val lastUpdatedAt: String,
)