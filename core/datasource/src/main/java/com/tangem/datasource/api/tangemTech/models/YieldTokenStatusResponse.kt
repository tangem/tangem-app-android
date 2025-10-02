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
    @Json(name = "isActive") val isActive: Boolean,
)