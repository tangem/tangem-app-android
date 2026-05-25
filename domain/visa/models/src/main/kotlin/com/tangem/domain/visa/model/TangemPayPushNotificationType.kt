package com.tangem.domain.visa.model

enum class TangemPayPushNotificationType(val value: String) {
    CARD_READY("card_ready"),
    TRANSACTION_SPEND("transaction_spend"),
    TOP_UP("declined_top_up"),
    COLLATERAL("collateral"),
    ;

    companion object {
        private val map = entries.associateBy { it.value }

        val all: Set<String> = entries.map { it.value }.toSet()

        fun fromValue(value: String): TangemPayPushNotificationType? = map[value]
    }
}