package com.tangem.domain.pay.model

import java.util.Locale

enum class OrderStep {
    AWAITING_DEPOSIT,
    AWAITING_SIGNATURE,
    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
            "AWAITING_DEPOSIT" -> AWAITING_DEPOSIT
            "AWAITING_SIGNATURE" -> AWAITING_SIGNATURE
            else -> UNKNOWN
        }
    }
}