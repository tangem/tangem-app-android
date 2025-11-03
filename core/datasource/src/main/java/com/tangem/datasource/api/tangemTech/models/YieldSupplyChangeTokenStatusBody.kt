package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YieldSupplyChangeTokenStatusBody(
    @Json(name = "tokenAddress") val tokenAddress: String,
    @Json(name = "chainId") val chainId: Int,
    @Json(name = "userAddress") val userAddress: String,
)