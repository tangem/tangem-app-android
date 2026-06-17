package com.tangem.datasource.api.onramp.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OnrampItemResponse(
    // region transaction info
    @Json(name = "txId")
    val txId: String,

    @Json(name = "providerId")
    val providerId: String,

    /** Address that received the target assets */
    @Json(name = "payoutAddress")
    val payoutAddress: String,

    /**
     * Raw backend status string, kept unparsed so a new value never breaks deserialization.
     * Typed view: [com.tangem.domain.express.models.ExpressOnrampStatus].
     */
    @Json(name = "status")
    val status: String,

    /** Failure reason reported by the provider */
    @Json(name = "failReason")
    val failReason: String?,

    /** External transaction ID reported by the provider in the webhook */
    @Json(name = "externalTxId")
    val externalTxId: String?,

    /** URL to view the transaction details on the provider side (not provided by all providers) */
    @Json(name = "externalTxUrl")
    val externalTxUrl: String?,

    /** Blockchain hash of the payout transaction */
    @Json(name = "payoutHash")
    val payoutHash: String?,

    /** Transaction creation timestamp in ISO-8601 format */
    @Json(name = "createdAt")
    val createdAt: String,

    /** Transaction last-update timestamp in ISO-8601 format */
    // todo txHistory uncomment
/*
    @Json(name = "updatedAt")
    val updatedAt: String,
*/
    // endregion

    // region fromAsset (fiat) info
    @Json(name = "fromCurrencyCode")
    val fromCurrencyCode: String,
    @Json(name = "fromAmount")
    val fromAmount: String,
    @Json(name = "fromPrecision")
    val fromPrecision: Int,
    // endregion

    // region toAsset info
    @Json(name = "toContractAddress")
    val toContractAddress: String,
    @Json(name = "toNetwork")
    val toNetwork: String,
    @Json(name = "toDecimals")
    val toDecimals: Int,

    /** Provider-promised amount, received from the provider in the webhook */
    @Json(name = "toAmount")
    val toAmount: String?,

    /** Actual amount delivered to the user, received from the provider in the webhook */
    @Json(name = "toActualAmount")
    val toActualAmount: String?,
    // endregion

    @Json(name = "paymentMethod")
    val paymentMethod: String,

    @Json(name = "countryCode")
    val countryCode: String,
)