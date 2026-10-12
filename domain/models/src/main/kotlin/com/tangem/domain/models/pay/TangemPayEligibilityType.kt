package com.tangem.domain.models.pay

enum class TangemPayEligibilityType {

    BANNER,
    DETAILS,
    DEEPLINK,

    VISA_VIRTUAL_ACCOUNT,

    BANK_OFFRAMP,

    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String): TangemPayEligibilityType = when (value.uppercase()) {
            "BANNER" -> BANNER
            "DETAILS" -> DETAILS
            "DEEPLINK" -> DEEPLINK
            "VISA_VIRTUAL_ACCOUNT" -> VISA_VIRTUAL_ACCOUNT
            "BANK_OFFRAMP" -> BANK_OFFRAMP
            else -> UNKNOWN
        }
    }
}

val TangemPayEligibilityType.isTangemPayType: Boolean
    get() = this in TANGEM_PAY_TYPES

private val TANGEM_PAY_TYPES = setOf(
    TangemPayEligibilityType.BANNER,
    TangemPayEligibilityType.DETAILS,
    TangemPayEligibilityType.DEEPLINK,
)