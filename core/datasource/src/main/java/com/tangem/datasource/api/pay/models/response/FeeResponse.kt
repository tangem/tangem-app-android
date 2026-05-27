package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FeeResponse(
    @Json(name = "result") val result: Result,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "type") val type: String,
        @Json(name = "amount") val amount: String,
        @Json(name = "currency") val currency: String,
        @Json(name = "description") val description: String,
    )
}