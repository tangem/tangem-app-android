package com.tangem.datasource.api.visa.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VisaErrorResponse(
    @Json(name = "error") val error: Error,
) {
    @JsonClass(generateAdapter = true)
    data class Error(
        @Json(name = "code") val code: Int,
    )
}