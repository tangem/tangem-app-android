package com.tangem.feature.swap.domain.models.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import org.joda.time.DateTime

@JsonClass(generateAdapter = true)
data class ExchangeStatusModel(
    @Json(name = "providerId")
    val providerId: String,
    @Json(name = "status")
    val status: ExchangeStatus? = null,
    @Json(name = "txId")
    val txId: String? = null,
    @Json(name = "txExternalUrl")
    val txExternalUrl: String? = null,
    @Json(name = "txExternalId")
    val txExternalId: String? = null,
    @Json(name = "refundNetwork")
    val refundNetwork: String? = null,
    @Json(name = "refundContractAddress")
    val refundContractAddress: String? = null,
    @Json(name = "refundTokensResponse")
    val refundTokensResponse: UserTokensResponse.Token? = null,
    @Json(ignore = true)
    val refundCurrency: CryptoCurrency? = null,
    @Json(name = "createdAt")
    val createdAt: DateTime? = null,
    @Json(name = "averageDuration")
    val averageDuration: Int? = null,
) {
    val hasLongTime: Boolean
        get() = if (createdAt != null && averageDuration != null) {
            DateTime.now().minusSeconds(averageDuration * 5) > createdAt
        } else {
            false
        }
}

@JsonClass(generateAdapter = false)
enum class ExchangeStatus {
    @Json(name = "New")
    New,

    @Json(name = "Waiting")
    Waiting,

    @Json(name = "WaitingTxHash")
    WaitingTxHash,

    @Json(name = "Confirming")
    Confirming,

    @Json(name = "Verifying")
    Verifying,

    @Json(name = "Exchanging")
    Exchanging,

    @Json(name = "Failed")
    Failed,

    @Json(name = "Sending")
    Sending,

    @Json(name = "Finished")
    Finished,

    @Json(name = "Refunded")
    Refunded,

    @Json(name = "Cancelled")
    Cancelled,

    @Json(name = "TxFailed")
    TxFailed,

    @Json(name = "Unknown")
    Unknown,

    @Json(name = "Paused")
    Paused,

    ;

    val isTerminal: Boolean
        get() = this == Refunded ||
            this == Finished ||
            this == Cancelled ||
            this == TxFailed ||
            this == Paused ||
            this == Unknown

    val isAutoDisposable: Boolean
        get() = this == Finished

    companion object {
        fun ExchangeStatus?.isFailed(): Boolean = this == Failed || this == TxFailed
    }
}