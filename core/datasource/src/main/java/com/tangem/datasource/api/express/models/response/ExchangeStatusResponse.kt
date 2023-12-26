package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json

data class ExchangeStatusResponse(

    @Json(name = "providerId")
    val providerId: String,

    @Json(name = "externalTxId")
    val externalTxId: String,

    @Json(name = "externalTxStatus")
    val externalStatus: ExchangeStatus,

    @Json(name = "externalTxUrl")
    val externalTxUrl: String,

    @Json(name = "error")
    val error: ExchangeStatusError?,
)

enum class ExchangeStatus {

    @Json(name = "new")
    NEW,

    @Json(name = "waiting")
    WAITING,

    @Json(name = "confirming")
    CONFIRMING,

    @Json(name = "exchanging")
    EXCHANGING,

    @Json(name = "sending")
    SENDING,

    @Json(name = "finished")
    FINISHED,

    @Json(name = "failed")
    FAILED,

    @Json(name = "refunded")
    REFUNDED,

    @Json(name = "verifying")
    VERIFYING,

    @Json(name = "cancelled")
    CANCELLED,
}

data class ExchangeStatusError(
    @Json(name = "code")
    val code: Int,

    @Json(name = "description")
    val description: String,
)