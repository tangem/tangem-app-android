package com.tangem.data.transaction.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The problem document the Transaction Service answers a failed intent request with; only its code is read. */
@Serializable
internal data class TransactionIntentErrorResponse(
    @SerialName("code") val code: TransactionIntentErrorCode = TransactionIntentErrorCode.UNKNOWN,
)