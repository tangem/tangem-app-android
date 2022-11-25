package com.tangem.datasource.api.oneinch.models

import com.squareup.moshi.Json

data class StatusResponse(
    @Json(name = "status") val status: String,
    @Json(name = "provider") val provider: String,
)
