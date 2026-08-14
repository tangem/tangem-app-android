package com.tangem.domain.visa.model

import com.tangem.domain.visa.model.TangemPayPushNotificationType.*

enum class TangemPayPushNotificationType {
    CARD_READY,
    TRANSACTION_SPEND,
    TRANSACTION_SPEND_REFUND,
    DECLINED_TOP_UP,
    DECLINED_REASON_1,
    DECLINED_REASON_2,
    DECLINED_REASON_3,
    DECLINED_REASON_4,
    DECLINED_REASON_5,
    DECLINED_REASON_6,
    DECLINED_REASON_7,
    DECLINED_REASON_8,
    DECLINED_REASON_9,
    DECLINED_REASON_10,
    DECLINED_REASON_11,
    DECLINED_REASON_12,
    DECLINED_REASON_13,
    DECLINED_REASON_14,
    DECLINED_REASON_15,
    DECLINED_REASON_16,
    DECLINED_REASON_17,
    COLLATERAL_WITHDRAW,
    COLLATERAL_DEPOSIT,
    THRESHOLD1_TOP_UP,
    ;

    enum class Action {
        CARD_DETAILS,
        SPEND_DETAILS,
        COLLATERAL_DETAILS,
        TOP_UP,
    }

    companion object {
        private val map = entries.associateBy { it.value() }

        val all: Set<String> = entries.map { it.value() }.toSet()

        fun fromValue(value: String): TangemPayPushNotificationType? = map[value]

        @Suppress("CyclomaticComplexMethod")
        private fun TangemPayPushNotificationType.value(): String = when (this) {
            CARD_READY -> "card_ready"
            TRANSACTION_SPEND -> "transaction_spend"
            TRANSACTION_SPEND_REFUND -> "transaction_spend_refund"
            DECLINED_TOP_UP -> "declined_top_up"
            DECLINED_REASON_1 -> "declined_reason1"
            DECLINED_REASON_2 -> "declined_reason2"
            DECLINED_REASON_3 -> "declined_reason3"
            DECLINED_REASON_4 -> "declined_reason4"
            DECLINED_REASON_5 -> "declined_reason5"
            DECLINED_REASON_6 -> "declined_reason6"
            DECLINED_REASON_7 -> "declined_reason7"
            DECLINED_REASON_8 -> "declined_reason8"
            DECLINED_REASON_9 -> "declined_reason9"
            DECLINED_REASON_10 -> "declined_reason10"
            DECLINED_REASON_11 -> "declined_reason11"
            DECLINED_REASON_12 -> "declined_reason12"
            DECLINED_REASON_13 -> "declined_reason13"
            DECLINED_REASON_14 -> "declined_reason14"
            DECLINED_REASON_15 -> "declined_reason15"
            DECLINED_REASON_16 -> "declined_reason16"
            DECLINED_REASON_17 -> "declined_reason17"
            COLLATERAL_WITHDRAW -> "collateral_withdraw"
            COLLATERAL_DEPOSIT -> "collateral_deposit"
            THRESHOLD1_TOP_UP -> "threshold1_top_up"
        }
    }
}

fun TangemPayPushNotificationType.action(): Action = when (this) {
    CARD_READY -> Action.CARD_DETAILS
    TRANSACTION_SPEND,
    TRANSACTION_SPEND_REFUND,
    DECLINED_TOP_UP,
    DECLINED_REASON_1,
    DECLINED_REASON_2,
    DECLINED_REASON_3,
    DECLINED_REASON_4,
    DECLINED_REASON_5,
    DECLINED_REASON_6,
    DECLINED_REASON_7,
    DECLINED_REASON_8,
    DECLINED_REASON_9,
    DECLINED_REASON_10,
    DECLINED_REASON_11,
    DECLINED_REASON_12,
    DECLINED_REASON_13,
    DECLINED_REASON_14,
    DECLINED_REASON_15,
    DECLINED_REASON_16,
    DECLINED_REASON_17,
    -> Action.SPEND_DETAILS
    COLLATERAL_WITHDRAW, COLLATERAL_DEPOSIT -> Action.COLLATERAL_DETAILS
    THRESHOLD1_TOP_UP -> Action.TOP_UP
}