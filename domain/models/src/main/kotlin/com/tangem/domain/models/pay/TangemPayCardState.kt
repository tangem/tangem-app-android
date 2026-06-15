package com.tangem.domain.models.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/**
 * Lifecycle state of a Tangem Pay card.
 */
@Serializable
enum class TangemPayCardState {
    /** Card is operational and ready to use. */
    @SerialName("Active")
    Active,

    /** A reissue order is in progress; the card is being replaced. */
    @SerialName("Reissuing")
    Reissuing,

    /** A close order is in progress; the card is being closed. */
    @SerialName("Closing")
    Closing,
    ;

    override fun toString() = when (this) {
        Active -> "Active"
        Reissuing -> "Reissuing"
        Closing -> "Closing"
    }

    companion object {
        fun fromString(value: String) = when (value.lowercase(Locale.US)) {
            "reissuing" -> Reissuing
            "closing" -> Closing
            else -> Active
        }
    }
}