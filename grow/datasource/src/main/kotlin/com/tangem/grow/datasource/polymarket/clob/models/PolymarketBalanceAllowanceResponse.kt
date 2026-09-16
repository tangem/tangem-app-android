package com.tangem.grow.datasource.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketBalanceAllowanceResponse(
    @Json(name = "balance") val balance: String,
    @Json(name = "allowances") val allowances: Map<String, String>?,
)