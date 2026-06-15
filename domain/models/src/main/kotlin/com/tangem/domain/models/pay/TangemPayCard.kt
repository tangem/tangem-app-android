package com.tangem.domain.models.pay

import com.tangem.domain.models.account.CardDisplayName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Tangem Pay card linked to a payment account.
 *
 * @property id unique card identifier assigned by the backend.
 * @property productInstanceId identifier of the owning product instance; used to join a card to its
 *           product instance and to scope card orders.
 * @property cardStatus backend card status; unknown values map to [Status.UNDEFINED].
 * @property hasPinCode whether the card has a PIN code set.
 * @property displayName optional human-readable name assigned to the card; `null` if not set.
 * @property limit spending limit configuration for the card; `null` if not configured or not yet loaded.
 * @property frozenState whether the card is currently frozen (blocked for payments).
 * @property lastDigits The last four digits of the card number.
 * @property state current lifecycle state of the card (reissuing / closing / active).
 */
@Serializable
data class TangemPayCard(
    @SerialName("id") val id: String,
    @SerialName("product_instance_id") val productInstanceId: String,
    @SerialName("card_status") val cardStatus: Status,
    @SerialName("has_pin_code") val hasPinCode: Boolean,
    @SerialName("display_name") val displayName: CardDisplayName?,
    @SerialName("limit") val limit: TangemPayCardLimitData?,
    @SerialName("frozen_state") val frozenState: TangemPayCardFrozenState,
    @SerialName("last_digits") val lastDigits: String,
    @SerialName("state") val state: TangemPayCardState,
) {

    /** Backend card status — unknown values map to [UNDEFINED] without crashing. */
    @Serializable
    enum class Status {
        @SerialName("ACTIVE")
        ACTIVE,

        @SerialName("INACTIVE")
        INACTIVE,

        @SerialName("BLOCKED")
        BLOCKED,

        @SerialName("CANCELED")
        CANCELED,

        @SerialName("UNDEFINED")
        UNDEFINED,
        ;

        val isActive: Boolean get() = this == ACTIVE

        companion object {
            fun fromString(value: String?): Status = when (value?.uppercase()) {
                "ACTIVE" -> ACTIVE
                "INACTIVE" -> INACTIVE
                "BLOCKED" -> BLOCKED
                "CANCELED" -> CANCELED
                else -> UNDEFINED
            }
        }
    }
}

val TangemPayCard.isFrozen
    get() = frozenState == TangemPayCardFrozenState.Frozen