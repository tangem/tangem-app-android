package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime

@JsonClass(generateAdapter = true)
data class ExchangeStatusResponse(

    @Json(name = "providerId")
    val providerId: String,

    @Json(name = "status")
    val status: ExchangeStatus,

    @Json(name = "externalTxId")
    val externalTxId: String?,

    @Json(name = "externalTxUrl")
    val externalTxUrl: String?,

    @Json(name = "error")
    val error: ExchangeStatusError?,

    @Json(name = "refundNetwork")
    val refundNetwork: String? = null,

    @Json(name = "refundContractAddress")
    val refundContractAddress: String? = null,

    @Json(name = "createdAt")
    val createdAt: DateTime? = null,

    @Json(name = "averageDuration")
    val averageDuration: Int? = null,
)

@JsonClass(generateAdapter = false)
enum class ExchangeStatus {

    @Json(name = "new")
    New,

    @Json(name = "waiting")
    Waiting,

    @Json(name = "confirming")
    Confirming,

    @Json(name = "exchanging")
    Exchanging,

    @Json(name = "sending")
    Sending,

    @Json(name = "finished")
    Finished,

    @Json(name = "failed")
    Failed,

    @Json(name = "refunded")
    Refunded,

    @Json(name = "verifying")
    Verifying,

    @Json(name = "expired")
    Cancelled,

    @Json(name = "waiting-tx-hash")
    WaitingTxHash,

    @Json(name = "tx-failed")
    TxFailed,

    @Json(name = "paused")
    Paused,

    @Json(name = "unknown")
    Unknown,
}

@JsonClass(generateAdapter = true)
data class ExchangeStatusError(
    @Json(name = "code")
    val code: Int,

    @Json(name = "description")
    val description: String,
)