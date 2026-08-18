package com.tangem.grow.datasource.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeSentResponseBody(
    @Json(name = "txId")
    val txId: String,
    @Json(name = "status")
    val status: String,
)