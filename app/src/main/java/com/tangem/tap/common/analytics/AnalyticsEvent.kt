package com.tangem.tap.common.analytics

enum class AnalyticsEvent(val event: String) {
    CARD_IS_SCANNED("card_is_scanned"),
    TRANSACTION_IS_SENT("transaction_is_sent"),
    READY_TO_SCAN("ready_to_scan"),
    APP_RATING_DISPLAYED("rate_app_warning_displayed"),
    APP_RATING_DISMISS("dismiss_rate_app_warning"),
    APP_RATING_NEGATIVE("negative_rate_app_feedback"),
    APP_RATING_POSITIVE("positive_rate_app_feedback"),
    WC_SUCCESS_RESPONSE("wallet_connect_success_response"),
    WC_INVALID_REQUEST("wallet_connect_invalid_request"),
    WC_NEW_SESSION("wallet_connect_new_session"),
    WC_SESSION_DISCONNECTED("wallet_connect_session_disconnected"),
}