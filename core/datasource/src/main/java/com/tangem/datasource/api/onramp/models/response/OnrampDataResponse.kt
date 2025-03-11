package com.tangem.datasource.api.onramp.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampDataResponseWithTxDetails(
    @Json(name = "dataResponse")
    val dataResponse: OnrampDataResponse,
    @Json(name = "txDetails")
    val txDetails: OnrampTxDetails,
)

@JsonClass(generateAdapter = true)
data class OnrampDataResponse(
    @Json(name = "txId")
    val txId: String,

    @Json(name = "dataJson")
    val dataJson: String,

    @Json(name = "signature")
    val signature: String,
)

@JsonClass(generateAdapter = true)
data class OnrampDataJson(
    @Json(name = "widgetUrl")
    val widgetUrl: String,

    @Json(name = "requestId")
    val requestId: String,

    @Json(name = "externalTxId")
    val externalTxId: String,

    @Json(name = "externalTxUrl")
    val externalTxUrl: String?,
)

@JsonClass(generateAdapter = true)
data class OnrampTxDetails(
    @Json(name = "fromCurrencyCode")
    val fromCurrencyCode: String,

    @Json(name = "toContractAddress")
    val toContractAddress: String,

    @Json(name = "toNetwork")
    val toNetwork: String,

    @Json(name = "paymentMethod")
    val paymentMethod: String,

    @Json(name = "countryCode")
    val countryCode: String,

    @Json(name = "fromAmount")
    val fromAmount: String,

    @Json(name = "toDecimals")
    val toDecimals: Int,

    @Json(name = "providerId")
    val providerId: String,

    @Json(name = "toAddress")
    val toAddress: String,

    @Json(name = "redirectUrl")
    val redirectUrl: String,

    @Json(name = "language")
    val language: String?,

    @Json(name = "theme")
    val theme: String?,

    @Json(name = "requestId")
    val requestId: String,

    @Json(name = "externalTxId")
    val externalTxId: String,

    @Json(name = "widgetUrl")
    val widgetUrl: String,
)