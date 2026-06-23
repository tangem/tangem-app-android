package com.tangem.domain.pay.model

import com.tangem.domain.pay.model.OrderType.Companion.fromString
import com.tangem.domain.pay.model.OrderType.Companion.issueCardTypes

/**
 * Order type used for findOrders filtering and order-conflict checks.
 *
 * Backend wire values are mapped via [fromString]; unknown values resolve to [UNKNOWN]
 * so the app never crashes on a new server-side type.
 */
enum class OrderType(val wireValue: String) {
    CARD_ISSUE_ADDITIONAL("CARD_ISSUE_ADDITIONAL"),
    CARD_ISSUE_VIRTUAL_RAIN("CARD_ISSUE_VIRTUAL_RAIN"),
    CARD_ISSUE_VIRTUAL_RAIN_KYC("CARD_ISSUE_VIRTUAL_RAIN_KYC"),
    CARD_ISSUE_VIRTUAL_RAIN_KYC_V2("CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"),
    CARD_REISSUE("CARD_REISSUE"),
    CARD_FREEZE("CARD_FREEZE"),
    CARD_UNFREEZE("CARD_UNFREEZE"),
    WITHDRAW("WITHDRAW"),
    UNKNOWN(""),
    ;

    /** `true` for any card-issuance order type — see [issueCardTypes]. */
    val isIssuing: Boolean get() = issueCardTypes.contains(this)

    /** `true` for card freeze / unfreeze orders. */
    val isFreezingUnfreezing: Boolean get() = this == CARD_FREEZE || this == CARD_UNFREEZE

    /** `true` for card reissue orders. */
    val isReissuing: Boolean get() = this == CARD_REISSUE

    companion object {

        /**
         * All order types that represent issuing a card: the first virtual card (and its KYC
         * variants) and an additional card. Used both to filter `findOrders` and to detect
         * issue-card conflicts.
         */
        val issueCardTypes = setOf(
            CARD_ISSUE_ADDITIONAL,
            CARD_ISSUE_VIRTUAL_RAIN,
            CARD_ISSUE_VIRTUAL_RAIN_KYC,
            CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
        )

        fun fromString(value: String?): OrderType {
            if (value.isNullOrBlank()) return UNKNOWN
            return entries.firstOrNull { it.wireValue == value || it.name == value } ?: UNKNOWN
        }
    }
}