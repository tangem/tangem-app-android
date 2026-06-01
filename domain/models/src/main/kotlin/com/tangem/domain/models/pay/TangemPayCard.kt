package com.tangem.domain.models.pay

import com.tangem.domain.models.account.CardDisplayName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a Tangem Pay card linked to a payment account.
 *
 * @property id unique card identifier assigned by the backend.
 * @property hasPinCode whether the card has a PIN code set.
 * @property displayName optional human-readable name assigned to the card; `null` if not set.
 * @property limit spending limit configuration for the card; `null` if not configured or not yet loaded.
 * @property frozenState whether the card is currently frozen (blocked for payments).
 * @property lastDigits The last four digits of the card number.
 * @property state current lifecycle state of the card.
 */
@Serializable
data class TangemPayCard(
    @SerialName("id") val id: String,
    @SerialName("has_pin_code") val hasPinCode: Boolean,
    @SerialName("display_name") val displayName: CardDisplayName?,
    @SerialName("limit") val limit: TangemPayCardLimitData?,
    @SerialName("frozen_state") val frozenState: TangemPayCardFrozenState,
    @SerialName("last_digits") val lastDigits: String,
    @SerialName("state") val state: TangemPayCardState,
)

val TangemPayCard.isFrozen
    get() = frozenState == TangemPayCardFrozenState.Frozen