package com.tangem.datasource.api.polymarket.clob.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PolymarketBalanceAllowanceResponse(
    @Json(name = "balance") val balance: String,
    @Json(name = "allowance") val allowance: String?,
)