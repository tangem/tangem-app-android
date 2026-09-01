package com.tangem.grow.datasource.stakekit.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SubmitTransactionHashRequestBody(
    @Json(name = "hash")
    val hash: String,
)