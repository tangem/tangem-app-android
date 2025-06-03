package com.tangem.domain.notifications.models

enum class NotificationType(val type: String) {
    Promo("promo"),
    IncomeTransactions("income_transaction"),
    SwapStatus("swap_status_update"),
    OnrampStatus("onramp_status_update"),
    Unknown("unknown"),
    ;

    companion object {
        fun getType(type: String?): NotificationType {
            return entries.firstOrNull { it.type == type } ?: Unknown
        }
    }
}