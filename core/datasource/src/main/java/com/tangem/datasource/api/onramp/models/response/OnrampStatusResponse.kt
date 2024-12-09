package com.tangem.datasource.api.onramp.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampStatusResponse(
    @Json(name = "txId")
    val txId: String,

    @Json(name = "providerId")
    val providerId: String,

    @Json(name = "payoutAddress")
    val payoutAddress: String,

    @Json(name = "status")
    val status: Status,

    @Json(name = "failReason")
    val failReason: String?,

    @Json(name = "externalTxId")
    val externalTxId: String,

    @Json(name = "externalTxUrl")
    val externalTxUrl: String?,

    @Json(name = "payoutHash")
    val payoutHash: String?,

    @Json(name = "createdAt")
    val createdAt: String,

    @Json(name = "fromCurrencyCode")
    val fromCurrencyCode: String,

    @Json(name = "fromAmount")
    val fromAmount: String,

    @Json(name = "toContractAddress")
    val toContractAddress: String,

    @Json(name = "toNetwork")
    val toNetwork: String,

    @Json(name = "toDecimals")
    val toDecimals: String,

    @Json(name = "toAmount")
    val toAmount: String?,

    @Json(name = "toActualAmount")
    val toActualAmount: String?,

    @Json(name = "paymentMethod")
    val paymentMethod: String,

    @Json(name = "countryCode")
    val countryCode: String,
)

enum class Status {
    @Json(name = "created")
    Created,

    @Json(name = "expired")
    Expired,

    @Json(name = "waiting-for-payment")
    WaitingForPayment,

    @Json(name = "payment-processing")
    PaymentProcessing,

    @Json(name = "verifying")
    Verifying,

    @Json(name = "failed")
    Failed,

    @Json(name = "paid")
    Paid,

    @Json(name = "sending")
    Sending,

    @Json(name = "finished")
    Finished,

    @Json(name = "paused")
    Paused,
}