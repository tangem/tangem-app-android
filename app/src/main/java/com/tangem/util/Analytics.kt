package com.tangem.util

enum class AnalyticsEvent(val event: String) {
    CARD_IS_SCANNED("card_is_scanned"),
    TRANSACTION_IS_SENT("transaction_is_sent"),
    ;
}

enum class AnalyticsParam(val param: String) {
    BLOCKCHAIN("blockchain"),
    BATCH_ID("batch_id"),
    FIRMWARE("firmware"),
    ;
}
