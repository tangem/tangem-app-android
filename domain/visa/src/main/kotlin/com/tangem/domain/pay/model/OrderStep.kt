package com.tangem.domain.pay.model

import java.util.Locale

enum class OrderStep {
    AWAITING_DEPOSIT,
    AWAITING_SIGNATURE,
    STARTING,
    INITIALIZATION,
    ELIGIBILITY_CHECK,
    RAIN_APPROVAL,
    AWAITING_KYC,
    ACCOUNT_ISSUE,
    ACCOUNT_ACTIVATION,
    UNKNOWN,
    ;

    companion object {
        fun fromString(value: String?) = when (value?.uppercase(Locale.US)) {
            "AWAITING_DEPOSIT" -> AWAITING_DEPOSIT
            "AWAITING_SIGNATURE" -> AWAITING_SIGNATURE
            "STARTING" -> STARTING
            "INITIALIZATION" -> INITIALIZATION
            "ELIGIBILITY_CHECK" -> ELIGIBILITY_CHECK
            "RAIN_APPROVAL" -> RAIN_APPROVAL
            "AWAITING_KYC" -> AWAITING_KYC
            "ACCOUNT_ISSUE" -> ACCOUNT_ISSUE
            "ACCOUNT_ACTIVATION" -> ACCOUNT_ACTIVATION
            else -> UNKNOWN
        }
    }
}