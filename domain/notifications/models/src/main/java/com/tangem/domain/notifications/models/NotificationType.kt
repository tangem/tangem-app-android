package com.tangem.domain.notifications.models

enum class NotificationType(val type: String) {
    Promo("promo"),
    Unknown("unknown"),
    ;

    companion object {
        fun getType(type: String?): NotificationType {
            return entries.firstOrNull { it.type == type } ?: Unknown
        }
    }
}