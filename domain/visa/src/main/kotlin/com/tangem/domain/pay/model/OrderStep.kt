package com.tangem.domain.pay.model

import java.util.Locale

enum class OrderStep {
    AWAITING_DEPOSIT,
    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
            "AWAITING_DEPOSIT" -> AWAITING_DEPOSIT
            else -> UNKNOWN
        }
    }
}