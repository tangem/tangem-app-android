package com.tangem.datasource.api.stakekit.models.response.model.transaction.tron

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TronStakeKitTransaction(
    @Json(name = "raw_data_hex")
    val rawDataHex: String,
)