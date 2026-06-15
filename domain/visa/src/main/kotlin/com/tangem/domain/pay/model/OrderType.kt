package com.tangem.domain.pay.model

import com.tangem.domain.pay.model.OrderType.Companion.fromString

/**
 * Order type used for findOrders filtering and order-conflict checks.
 *
 * Backend wire values are mapped via [fromString]; unknown values resolve to [UNKNOWN]
 * so the app never crashes on a new server-side type.
 */
enum class OrderType(val wireValue: String) {
    CARD_ISSUE("CARD_ISSUE_VIRTUAL_RAIN_KYC"),
    CARD_ISSUE_ADDITIONAL("CARD_ISSUE_ADDITIONAL"),
    CARD_ISSUE_VIRTUAL_RAIN_KYC_V2("CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"),
    CARD_REISSUE("CARD_REISSUE"),
    CARD_FREEZE("CARD_FREEZE"),
    CARD_UNFREEZE("CARD_UNFREEZE"),
    WITHDRAW("WITHDRAW"),
    UNKNOWN(""),
    ;

    companion object {
        fun fromString(value: String?): OrderType {
            if (value.isNullOrBlank()) return UNKNOWN
            return entries.firstOrNull { it.wireValue == value || it.name == value } ?: UNKNOWN
        }
    }
}