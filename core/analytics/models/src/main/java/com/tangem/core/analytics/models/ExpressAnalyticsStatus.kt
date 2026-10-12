package com.tangem.core.analytics.models

enum class ExpressAnalyticsStatus(val value: String) {
    WaitingTxHash("Waiting tx hash"),
    InProgress("In Progress"),
    Done("Done"),
    Fail("Fail"),
    FailTx("Fail tx"),
    Unknown("Unknown"),
    KYC("KYC"),
    Refunded("Refunded"),
    Cancelled("Canceled"),
}