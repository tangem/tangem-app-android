package com.tangem.datasource.api.stakekit.models.request

import com.squareup.moshi.Json

data class SubmitTransactionHashRequestBody(
    @Json(name = "hash")
    val hash: String,
)