package com.tangem.domain.pay.model

import java.util.Locale

enum class OrderStatus {
    UNKNOWN, // TODO remove it after TangemPay accounts refactor TANGEM_PAY_ACCOUNTS_REFACTOR_ENABLED
    NEW,
    PROCESSING,
    COMPLETED,
    CANCELED,
    ;

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