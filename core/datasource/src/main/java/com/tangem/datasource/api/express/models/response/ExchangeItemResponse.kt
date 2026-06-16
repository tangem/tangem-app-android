package com.tangem.datasource.api.express.models.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExchangeItemResponse(
    // region transaction info
    @Json(name = "txId")
    val txId: String,

    @Json(name = "providerId")
    val providerId: String,

    /**
     * Address from which the `from` assets were taken for the exchange. Optional because the very first
     * app versions did not send it; for newer versions it can be considered effectively mandatory.
     */
    @Json(name = "fromAddress")
    val fromAddress: String?,

    /** Address to which the source assets were transferred for the exchange */
    @Json(name = "payinAddress")
    val payinAddress: String,

    /** Extra ID used for the pay-in transaction */
    @Json(name = "payinExtraId")
    val payinExtraId: String?,

    /** Address that received the target assets */
    @Json(name = "payoutAddress")
    val payoutAddress: String,

    /** Refund destination address */
    @Json(name = "refundAddress")
    val refundAddress: String?,

    /** Extra ID used for refunds */
    @Json(name = "refundExtraId")
    val refundExtraId: String?,

    /** Exchange rate type (e.g. float, fixed) */
    @Json(name = "rateType")
    val rateType: String,

    /**
     * Raw backend status string, kept unparsed so a new value never breaks deserialization.
     * Typed view: [com.tangem.domain.express.models.ExpressExchangeStatus].
     */
    @Json(name = "status")
    val status: String,

    /** External transaction ID (CEX only) */
    @Json(name = "externalTxId")
    val externalTxId: String?,

    /** URL to view the transaction details (CEX only) */
    @Json(name = "externalTxUrl")
    val externalTxUrl: String?,

    /** Blockchain hash of the pay-in transaction */
    @Json(name = "payinHash")
    val payinHash: String?,

    /** Blockchain hash of the payout transaction */
    @Json(name = "payoutHash")
    val payoutHash: String?,

    /** Network used for the refund transaction */
    @Json(name = "refundNetwork")
    val refundNetwork: String?,

    /** Refunded token contract address */
    @Json(name = "refundContractAddress")
    val refundContractAddress: String?,

    /** Transaction creation timestamp in ISO-8601 format */
    @Json(name = "createdAt")
    val createdAt: String,

    /** Transaction last-update timestamp in ISO-8601 format */
    @Json(name = "updatedAt")
    val updatedAt: String,

    /** Pay-in expiration timestamp in ISO-8601 format */
    @Json(name = "payTill")
    val payTill: String?,

    /** Average provider exchange duration in seconds */
    @Json(name = "averageDuration")
    val averageDuration: Long?,
    // endregion

    // region fromAsset info
    @Json(name = "fromContractAddress")
    val fromContractAddress: String,
    @Json(name = "fromNetwork")
    val fromNetwork: String,
    @Json(name = "fromDecimals")
    val fromDecimals: Int,
    @Json(name = "fromAmount")
    val fromAmount: String,
    // endregion

    // region toAsset info
    @Json(name = "toContractAddress")
    val toContractAddress: String,
    @Json(name = "toNetwork")
    val toNetwork: String,
    @Json(name = "toDecimals")
    val toDecimals: Int,
    @Json(name = "toAmount")
    val toAmount: String,
    @Json(name = "toActualAmount")
    val toActualAmount: String?,
    // endregion
)