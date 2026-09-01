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

    /** An issue order is in progress; the (additional) card is being issued and not yet provisioned. */
    @SerialName("Issuing")
    Issuing,

    @SerialName("Delivering")
    Delivering,

    /** The last-4 activation order for a delivered physical card is still being processed. */
    @SerialName("Activating")
    Activating,
    ;

    override fun toString() = when (this) {
        Active -> "Active"
        Reissuing -> "Reissuing"
        Closing -> "Closing"
        Issuing -> "Issuing"
        Delivering -> "Delivering"
        Activating -> "Activating"
    }

    companion object {
        fun fromString(value: String) = when (value.lowercase(Locale.US)) {
            "reissuing" -> Reissuing
            "closing" -> Closing
            "issuing" -> Issuing
            "delivering" -> Delivering
            "activating" -> Activating
            else -> Active
        }
    }
}

/**
 * A physical card that has been produced but is not usable yet — either still in delivery, or with an
 * activation order in flight. Such a card reports [TangemPayCardFrozenState.Frozen] because its product
 * instance is not `ACTIVE` yet, so callers must not render it as frozen or expose its real card data.
 */
val TangemPayCardState.isAwaitingActivation: Boolean
    get() = this == TangemPayCardState.Delivering || this == TangemPayCardState.Activating