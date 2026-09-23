package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response of `bff-v2/v1/waitlist`. */
@JsonClass(generateAdapter = true)
data class WaitlistResponse(
    @Json(name = "result") val result: Result,
) {

    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "status") val status: String,
        @Json(name = "joined_at") val joinedAt: String?,
    )
}