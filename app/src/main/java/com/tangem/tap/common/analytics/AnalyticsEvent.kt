package com.tangem.tap.common.analytics

enum class AnalyticsEvent(val event: String) {
    CARD_IS_SCANNED("card_is_scanned"),
    TRANSACTION_IS_SENT("transaction_is_sent"),
    READY_TO_SCAN("ready_to_scan"),
}