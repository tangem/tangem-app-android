package com.tangem.domain.swap.models

import com.tangem.datasource.api.tangemTech.models.UserTokensResponse
import com.tangem.domain.models.currency.CryptoCurrency
import org.joda.time.DateTime

/**
 * Swapped status model
 */
data class SwapStatusModel(
    val providerId: String,
    val status: SwapStatus? = null,
    val txId: String? = null,
    val txExternalUrl: String? = null,
    val txExternalId: String? = null,
    val refundNetwork: String? = null,
    val refundContractAddress: String? = null,
    val refundTokensResponse: UserTokensResponse.Token? = null,
    val refundCurrency: CryptoCurrency? = null,
    val createdAt: DateTime? = null,
    val averageDuration: Int? = null,
) {
    val hasLongTime: Boolean
        get() = if (createdAt != null && averageDuration != null) {
            DateTime.now().minusSeconds(averageDuration * 5) > createdAt
        } else {
            false
        }
}

enum class SwapStatus {
    New,
    Waiting,
    WaitingTxHash,
    Confirming,
    Verifying,
    Exchanging,
    Failed,
    Sending,
    Finished,
    Refunded,
    Cancelled,
    TxFailed,
    Unknown,
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
        fun SwapStatus?.isFailed(): Boolean = this == Failed || this == TxFailed
    }
}