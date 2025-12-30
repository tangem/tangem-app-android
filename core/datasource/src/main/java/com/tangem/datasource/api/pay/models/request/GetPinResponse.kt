package com.tangem.datasource.api.pay.models.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetPinResponse(@Json(name = "result") val result: Result?) {

    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "secret") val secret: String,
        @Json(name = "iv") val iv: String,
    )
}