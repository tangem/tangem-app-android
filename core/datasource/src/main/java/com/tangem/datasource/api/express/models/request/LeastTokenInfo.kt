package com.tangem.datasource.api.express.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LeastTokenInfo(
    @Json(name = "contractAddress")
    val contractAddress: String,

    @Json(name = "network")
    val network: String,
)