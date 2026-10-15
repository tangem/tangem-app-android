package com.tangem.data.transaction.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The `code` of a failed intent request; only the codes the app reacts to are named. */
@Serializable
internal enum class TransactionIntentErrorCode {
    @SerialName("INTENT_NOT_FOUND")
    INTENT_NOT_FOUND,

    @SerialName("INTENT_SESSION_REVOKED")
    INTENT_SESSION_REVOKED,

    @SerialName("INTENT_ALREADY_COMPLETED")
    INTENT_ALREADY_COMPLETED,

    /** Any other code; the network Json coerces an unknown value to it. */
    UNKNOWN,
}