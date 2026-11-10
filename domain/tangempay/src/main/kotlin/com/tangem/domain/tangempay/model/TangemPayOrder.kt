package com.tangem.domain.tangempay.model

import java.util.Locale

data class TangemPayOrder(
    val id: String,
    val status: Status,
    val step: OrderStep,
    val details: OrderDetails,
) {

    enum class Type {
        CARD_ISSUE_VIRTUAL,
        CARD_ISSUE_VIRTUAL_KYC,
        CARD_ISSUE_VIRTUAL_KYC_V2,
        CARD_ISSUE_PLASTIC,
        CARD_ACTIVATION_PLASTIC,
        CARD_REISSUE_PLASTIC,
        CARD_REISSUE_VIRTUAL,
        CARD_FREEZE,
        CARD_UNFREEZE,
        CARD_CLOSE,
        ACCOUNT_ISSUE_VIRTUAL,
        ACCOUNT_ISSUE_BANKING,
        WITHDRAWAL,
        BANK_OFFRAMP,
        SMART_CONTRACT_ISSUE,
        TARIFF_PLAN_TRANSITION,
        ONBOARDING,
        UNDEFINED,
    }

    enum class Status {
        NEW,
        PROCESSING,
        COMPLETED,
        CANCELED,
        UNKNOWN,
        ;

        val isTerminal: Boolean get() = this == COMPLETED || this == CANCELED

        companion object {
            fun fromString(value: String) = when (value.uppercase(Locale.US)) {
                "NEW" -> NEW
                "PROCESSING" -> PROCESSING
                "COMPLETED" -> COMPLETED
                "CANCELED" -> CANCELED
                else -> UNKNOWN
            }
        }
    }
}