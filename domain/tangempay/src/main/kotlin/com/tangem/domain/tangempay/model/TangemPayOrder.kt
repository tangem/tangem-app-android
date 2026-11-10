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
        ;

        @Suppress("CyclomaticComplexMethod")
        fun toTypeId(): String = when (this) {
            CARD_ISSUE_VIRTUAL -> "CARD_ISSUE_VIRTUAL_RAIN"
            CARD_ISSUE_VIRTUAL_KYC -> "CARD_ISSUE_VIRTUAL_RAIN_KYC"
            CARD_ISSUE_VIRTUAL_KYC_V2 -> "CARD_ISSUE_VIRTUAL_RAIN_KYC_V2"
            CARD_ISSUE_PLASTIC -> "CARD_ISSUE_PLASTIC_RAIN"
            CARD_ACTIVATION_PLASTIC -> "CARD_ACTIVATION_PLASTIC_RAIN"
            CARD_REISSUE_PLASTIC -> "CARD_REISSUE_PLASTIC_RAIN"
            CARD_REISSUE_VIRTUAL -> "CARD_REISSUE_VIRTUAL_RAIN"
            CARD_FREEZE -> "CARD_FREEZE_RAIN"
            CARD_UNFREEZE -> "CARD_UNFREEZE_RAIN"
            CARD_CLOSE -> "CARD_CLOSE_RAIN"
            ACCOUNT_ISSUE_VIRTUAL -> "ACCOUNT_ISSUE_VIRTUAL_RAIN"
            ACCOUNT_ISSUE_BANKING -> "ACCOUNT_ISSUE_BANKING_RAIN"
            WITHDRAWAL -> "WITHDRAWAL_RAIN"
            BANK_OFFRAMP -> "BANK_OFFRAMP_RAIN"
            SMART_CONTRACT_ISSUE -> "SMART_CONTRACT_ISSUE_RAIN"
            TARIFF_PLAN_TRANSITION -> "TARIFF_PLAN_TRANSITION"
            ONBOARDING -> "ONBOARDING_RAIN"
            UNDEFINED -> "UNDEFINED"
        }
    }

    enum class Status {
        NEW,
        PROCESSING,
        COMPLETED,
        CANCELED,
        UNKNOWN,
        ;

        val isTerminal: Boolean get() = this == COMPLETED || this == CANCELED

        fun toStatusId(): String? = when (this) {
            NEW -> "NEW"
            PROCESSING -> "PROCESSING"
            COMPLETED -> "COMPLETED"
            CANCELED -> "CANCELED"
            UNKNOWN -> null
        }

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