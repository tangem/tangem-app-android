package com.tangem.datasource.api.common.blockaid.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ValidationResponse(
    @Json(name = "status") val status: String,
    @Json(name = "result_type") val resultType: String,
)