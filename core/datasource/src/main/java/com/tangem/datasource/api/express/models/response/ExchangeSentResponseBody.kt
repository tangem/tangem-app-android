package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json

data class ExchangeSentResponseBody(
    @Json(name = "txId")
    val txId: String,
    @Json(name = "status")
    val status: String,
)