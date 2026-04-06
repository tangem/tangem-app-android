package com.tangem.domain.models

enum class TangemPayEligibilityType {

    BANNER,
    DETAILS,
    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String): TangemPayEligibilityType = when (value.lowercase()) {
            "banner" -> BANNER
            "details" -> DETAILS
            else -> UNKNOWN
        }
    }
}