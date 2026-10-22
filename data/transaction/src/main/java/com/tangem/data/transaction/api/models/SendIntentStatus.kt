package com.tangem.data.transaction.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class SendIntentStatus {
    @SerialName("CREATED")
    CREATED,

    @SerialName("COMPLETED")
    COMPLETED,

    /** A status the app does not know yet; the network Json coerces any unknown value to it. */
    UNKNOWN,
}