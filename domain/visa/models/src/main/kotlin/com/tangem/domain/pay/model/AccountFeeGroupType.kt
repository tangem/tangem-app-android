package com.tangem.domain.pay.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
enum class AccountFeeGroupType {
    @SerialName("ATM")
    ATM,

    @SerialName("CARD")
    CARD,

    @SerialName("ONRAMP")
    ONRAMP,

    @SerialName("OFFRAMP")
    OFFRAMP,

    @SerialName("UNKNOWN")
    UNKNOWN,
    ;

    override fun toString(): String = when (this) {
        CARD -> "CARD"
        ATM -> "ATM"
        ONRAMP -> "ONRAMP"
        OFFRAMP -> "OFFRAMP"
        UNKNOWN -> "UNKNOWN"
    }

    companion object {
        fun fromString(value: String?): AccountFeeGroupType = when (value?.uppercase(Locale.US)) {
            "CARD" -> CARD
            "ATM" -> ATM
            "ONRAMP" -> ONRAMP
            "OFFRAMP" -> OFFRAMP
            else -> UNKNOWN
        }
    }
}