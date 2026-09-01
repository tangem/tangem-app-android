package com.tangem.domain.pay.model

import com.tangem.domain.pay.model.OrderType.Companion.fromString
import com.tangem.domain.pay.model.OrderType.Companion.issueCardTypes

/**
 * Order type used for findOrders filtering.
 *
 * Backend wire values are mapped via [fromString]; unknown values resolve to [UNKNOWN]
 * so the app never crashes on a new server-side type.
 */
enum class OrderType(val wireValue: String) {
    CARD_ISSUE_ADDITIONAL("CARD_ISSUE_ADDITIONAL"),
    CARD_ISSUE_VIRTUAL_RAIN("CARD_ISSUE_VIRTUAL_RAIN"),
    CARD_ISSUE_VIRTUAL_RAIN_KYC("CARD_ISSUE_VIRTUAL_RAIN_KYC"),
    CARD_ISSUE_VIRTUAL_RAIN_KYC_V2("CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"),
    CARD_ISSUE_PLASTIC_RAIN("CARD_ISSUE_PLASTIC_RAIN"),
    CARD_ACTIVATION_PLASTIC_RAIN("CARD_ACTIVATION_PLASTIC_RAIN"),
    CARD_REISSUE("CARD_REISSUE"),
    CARD_REISSUE_PLASTIC_RAIN("CARD_REISSUE_PLASTIC_RAIN"),
    CARD_FREEZE("CARD_FREEZE"),
    CARD_UNFREEZE("CARD_UNFREEZE"),
    WITHDRAW("WITHDRAW"),
    TARIFF_PLAN_TRANSITION("TARIFF_PLAN_TRANSITION"),
    SMART_CONTRACT_ISSUE_RAIN("SMART_CONTRACT_ISSUE_RAIN"),
    UNKNOWN(""),
    ;

    /** `true` for any card-issuance order type — see [issueCardTypes]. */
    val isIssuing: Boolean get() = issueCardTypes.contains(this)

    companion object {

        /**
         * All order types that represent issuing a card: the virtual card, its KYC variants, and the
         * plastic one. Used to filter `findOrders` and to restore in-flight issuance.
         */
        val issueCardTypes = setOf(
            CARD_ISSUE_VIRTUAL_RAIN,
            CARD_ISSUE_VIRTUAL_RAIN_KYC,
            CARD_ISSUE_VIRTUAL_RAIN_KYC_V2,
            CARD_ISSUE_PLASTIC_RAIN,
        )

        fun fromString(value: String?): OrderType {
            if (value.isNullOrBlank()) return UNKNOWN
            return entries.firstOrNull { it.wireValue == value || it.name == value } ?: UNKNOWN
        }
    }
}