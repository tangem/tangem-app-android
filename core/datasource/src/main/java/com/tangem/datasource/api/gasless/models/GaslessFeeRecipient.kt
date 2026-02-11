package com.tangem.datasource.api.gasless.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GaslessFeeRecipient(
    @Json(name = "feeRecipientAddress") val address: String,
)