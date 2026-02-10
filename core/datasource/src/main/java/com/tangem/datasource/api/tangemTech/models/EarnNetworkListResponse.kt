package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class EarnNetworkListResponse(
    @Json(name = "items") val items: List<EarnNetworkResponse>,
)

@JsonClass(generateAdapter = true)
data class EarnNetworkResponse(
    @Json(name = "networkId") val networkId: String,
)