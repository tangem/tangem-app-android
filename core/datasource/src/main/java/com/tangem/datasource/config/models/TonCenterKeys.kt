package com.tangem.datasource.config.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TonCenterKeys(
    @Json(name = "mainnet")
    val mainnet: String,
    @Json(name = "testnet")
    val testnet: String,
)
