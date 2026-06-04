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

    /** Address from which the source assets were taken for the exchange */
    @Json(name = "fromAddress")
    val fromAddress: String,

    /** Address to which the assets were transferred for the exchange */
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

    /** Exchange rate type used in the transaction (float, fixed) */
    @Json(name = "rateType")
    val rateType: String,

    @Json(name = "status")
    val status: Status,

    /** External transaction ID (CEX only) */
    @Json(name = "externalTxId")
    val externalTxId: String?,

    /** Transaction status reported by the provider */
    @Json(name = "externalTxStatus")
    val externalTxStatus: String?,

    /** URL to view the transaction details (CEX only) */
    @Json(name = "externalTxUrl")
    val externalTxUrl: String?,

    /** Blockchain hash of the pay-in transaction */
    @Json(name = "payinHash")
    val payinHash: String?,

    /** Blockchain hash of the payout transaction */
    @Json(name = "payoutHash")
    val payoutHash: String?,

    /** Network used for the refund transaction (when status is refunded) */
    @Json(name = "refundNetwork")
    val refundNetwork: String?,

    /** Refunded token contract address */
    @Json(name = "refundContractAddress")
    val refundContractAddress: String?,

    /** Transaction creation timestamp in ISO-8601 format */
    @Json(name = "createdAt")
    val createdAt: String,

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
) {

    enum class Status {

        @Json(name = "unknown")
        UNKNOWN,

        @Json(name = "exchange-tx-sent")
        EXCHANGE_TX_SENT,

        @Json(name = "waiting")
        WAITING,

        @Json(name = "waiting-tx-hash")
        WAITING_TX_HASH,

        @Json(name = "expired")
        EXPIRED,

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

        @Json(name = "tx-failed")
        TX_FAILED,

        @Json(name = "refunded")
        REFUNDED,

        @Json(name = "verifying")
        VERIFYING,

        @Json(name = "paused")
        PAUSED,
    }
}