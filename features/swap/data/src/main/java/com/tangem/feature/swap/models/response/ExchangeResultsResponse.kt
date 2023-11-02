package com.tangem.feature.swap.models.response

import com.squareup.moshi.Json

data class ExchangeResultsResponse(
    @Json(name = "status")
    val status: ExchangeResultsStatus,

    @Json(name = "externalStatus")
    val externalStatus: String,

    @Json(name = "externalTxUrl")
    val externalTxUrl: String,

    @Json(name = "error")
    val error: ExchangeResultsError?,
)

enum class ExchangeResultsStatus {
    @Json(name = "processing")
    PROCESSING,

    @Json(name = "done")
    DONE,

    @Json(name = "failed")
    FAILED,

    @Json(name = "refunded")
    REFUNDED,

    @Json(name = "verificationRequired")
    VERIFICATION_REQUIRED,
}

data class ExchangeResultsError(
    @Json(name = "code")
    val code: Int,

    @Json(name = "description")
    val description: String,
)