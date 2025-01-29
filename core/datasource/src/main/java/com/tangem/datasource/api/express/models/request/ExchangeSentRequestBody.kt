package com.tangem.datasource.api.express.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeSentRequestBody(
    @Json(name = "txId")
    val txId: String,
    @Json(name = "fromNetwork")
    val fromNetwork: String,
    @Json(name = "fromAddress")
    val fromAddress: String,
    @Json(name = "payinAddress")
    val payinAddress: String,
    @Json(name = "payinExtraId")
    val payinExtraId: String?,
    @Json(name = "txHash")
    val txHash: String,
)