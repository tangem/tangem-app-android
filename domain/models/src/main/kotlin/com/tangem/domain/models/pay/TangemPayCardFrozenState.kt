package com.tangem.domain.models.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

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
        fun fromString(value: String) = when (value.lowercase(Locale.US)) {
            "frozen" -> Frozen
            "unfrozen" -> Unfrozen
            else -> Pending
        }
    }
}