package com.tangem.store.datasource.blockaid.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DomainScanRequest(
    @Json(name = "url") val url: String,
)