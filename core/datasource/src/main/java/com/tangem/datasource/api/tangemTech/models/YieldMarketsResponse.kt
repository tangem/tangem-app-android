package com.tangem.datasource.api.tangemTech.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class YieldMarketsResponse(
    @Json(name = "tokens") val marketDtos: List<YieldSupplyMarketTokenDto>,
    @Json(name = "lastUpdatedAt") val lastUpdated: String,
)