package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YieldModuleStatusResponse(
    @Json(name = "tokenAddress") val tokenAddress: String,
    @Json(name = "chainId") val chainId: Int,
    @Json(name = "isActive") val isActive: Boolean,
    @Json(name = "activatedAt") val activatedAt: String?,
    @Json(name = "deactivatedAt") val deactivatedAt: String?,
)