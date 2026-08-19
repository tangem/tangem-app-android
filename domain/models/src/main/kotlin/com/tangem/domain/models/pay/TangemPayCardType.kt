package com.tangem.domain.models.pay

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale

/** Form factor of a Tangem Pay card. */
@Serializable
enum class TangemPayCardType {
    @SerialName("VIRTUAL")
    VIRTUAL,

    @SerialName("PHYSICAL")
    PHYSICAL,

    @SerialName("UNDEFINED")
    UNDEFINED,
    ;

    companion object {
        fun fromString(value: String?): TangemPayCardType = when (value?.uppercase(Locale.US)) {
            "VIRTUAL" -> VIRTUAL
            "PHYSICAL" -> PHYSICAL
            else -> UNDEFINED
        }
    }
}