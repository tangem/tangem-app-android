package com.tangem.spend.datasource.pay.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response of `bff-v2/v1/eligibility/check`
 */
@JsonClass(generateAdapter = true)
data class EligibilityCheckResponse(
    @Json(name = "result") val result: Result,
) {

    @JsonClass(generateAdapter = true)
    data class Result(
        @Json(name = "available") val isAvailable: Boolean,
        @Json(name = "country") val country: String,
        @Json(name = "region") val region: String?,
    )
}