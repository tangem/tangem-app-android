package com.tangem.datasource.api.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WithdrawDataResponse(
    @Json(name = "result") val result: Result?,
) {
    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "hash") val hash: String,
        @Json(name = "salt") val salt: String,
        @Json(name = "sender_address") val senderAddress: String,
    )
}