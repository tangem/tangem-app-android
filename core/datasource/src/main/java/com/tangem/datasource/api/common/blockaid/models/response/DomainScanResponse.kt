package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DomainScanResponse(
    @Json(name = "status") val status: String,
    @Json(name = "is_malicious") val isMalicious: Boolean?,
)