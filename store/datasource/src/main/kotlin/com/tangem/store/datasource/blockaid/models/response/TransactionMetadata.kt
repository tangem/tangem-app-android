package com.tangem.store.datasource.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TransactionMetadata(
    @Json(name = "domain") val domain: String,
)