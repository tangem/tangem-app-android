package com.tangem.data.transaction.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ConfirmIntentTransactionRequest(
    /** At most 128 characters. */
    @SerialName("txHash") val txHash: String,
)