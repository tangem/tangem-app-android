package com.tangem.domain.models.pay

enum class TangemPayEligibilityType {

    BANNER,
    DETAILS,
    DEEPLINK,

    BANNER_VIRTUAL_ACCOUNT,
    DETAILS_VIRTUAL_ACCOUNT,
    DEEPLINK_VIRTUAL_ACCOUNT,

    VISA_VIRTUAL_ACCOUNT,

    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String): TangemPayEligibilityType = when (value.uppercase()) {
            "BANNER" -> BANNER
            "DETAILS" -> DETAILS
            "DEEPLINK" -> DEEPLINK
            "BANNER_VIRTUAL_ACCOUNT" -> BANNER_VIRTUAL_ACCOUNT
            "DETAILS_VIRTUAL_ACCOUNT" -> DETAILS_VIRTUAL_ACCOUNT
            "DEEPLINK_VIRTUAL_ACCOUNT" -> DEEPLINK_VIRTUAL_ACCOUNT
            "VISA_VIRTUAL_ACCOUNT" -> VISA_VIRTUAL_ACCOUNT
            else -> UNKNOWN
        }
    }
}

val TangemPayEligibilityType.isVirtualAccountType: Boolean
    get() = this in VIRTUAL_ACCOUNT_TYPES

val TangemPayEligibilityType.isTangemPayType: Boolean
    get() = this in TANGEM_PAY_TYPES

private val VIRTUAL_ACCOUNT_TYPES = setOf(
    TangemPayEligibilityType.BANNER_VIRTUAL_ACCOUNT,
    TangemPayEligibilityType.DETAILS_VIRTUAL_ACCOUNT,
    TangemPayEligibilityType.DEEPLINK_VIRTUAL_ACCOUNT,
)

private val TANGEM_PAY_TYPES = setOf(
    TangemPayEligibilityType.BANNER,
    TangemPayEligibilityType.DETAILS,
    TangemPayEligibilityType.DEEPLINK,
)