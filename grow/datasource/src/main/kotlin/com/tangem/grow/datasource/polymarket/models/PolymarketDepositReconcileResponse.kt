package com.tangem.grow.datasource.polymarket.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketDepositReconcileResponse(
    @Json(name = "state") val state: String,
    @Json(name = "retryAfterSeconds") val retryAfterSeconds: Long,
)