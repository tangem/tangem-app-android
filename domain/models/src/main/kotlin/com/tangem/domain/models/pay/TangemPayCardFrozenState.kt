package com.tangem.domain.models.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TangemPayCardFrozenState {
    @SerialName("Pending")
    Pending,

    @SerialName("Frozen")
    Frozen,

    @SerialName("Unfrozen")
    Unfrozen,
    ;

    override fun toString() = when (this) {
        Pending -> "Pending"
        Frozen -> "Frozen"
        Unfrozen -> "Unfrozen"
    }

    companion object {
        fun fromString(value: String) = when (value) {
            "Frozen" -> Frozen
            "Unfrozen" -> Unfrozen
            else -> Pending
        }
    }
}