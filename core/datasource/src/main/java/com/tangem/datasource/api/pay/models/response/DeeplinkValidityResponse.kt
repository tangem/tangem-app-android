package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeeplinkValidityResponse(
    @Json(name = "result") val result: Result?,
    @Json(name = "error") val error: String?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "status") val status: String,
    )
}