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

    GET_CARD("get_card"),
}

enum class AnalyticsParam(val param: String) {
    BLOCKCHAIN("blockchain"),
    BATCH_ID("batch_id"),
    FIRMWARE("firmware"),
    ACTION("action"),
    ERROR_DESCRIPTION("error_description"),
    ERROR_CODE("error_code"),
    NEW_SECURITY_OPTION("new_security_option"),
    ERROR_KEY("Tangem SDK error key"),

    WALLET_CONNECT_ACTION("wallet_connect_action"),
    WALLET_CONNECT_REQUEST("wallet_connect_request"),
    WALLET_CONNECT_DAPP_URL("wallet_connect_dapp_url"),

    SOURCE("source"),
}

enum class GetCardSourceParams(val param: String) {
    WELCOME("welcome"),
    ONBOARDING_BUY_MORE("wallet_onboaring_buy_more_cards"),
    ONBOARDING("wallet_onboarding")
}

object Analytics {
    enum class ActionToLog(val key: String) {
        Scan("tap_scan_task"),
        SendTransaction("send_transaction"),
        WalletConnectSign("wallet_connect_personal_sign"),
        WalletConnectTransaction("wallet_connect_tx_sign"),
        ReadPinSettings("read_pin_settings"),
        ChangeSecOptions("change_sec_options"),
        CreateWallet("create_wallet"),
        PurgeWallet("purge_wallet"),
        WriteIssuerData("write_issuer_data"),
    }

    enum class WcSessionEvent { Disconnect, Connect }

    enum class WcAction { PersonalSign, SignTransaction, SendTransaction }

    sealed class WcAnalyticsEvent {
        data class Error(val error: Throwable, val action: WcAction?) :
            WcAnalyticsEvent()

        data class Session(val event: WcSessionEvent, val url: String?) : WcAnalyticsEvent()
        data class Action(val action: WcAction) : WcAnalyticsEvent()
        data class InvalidRequest(val json: String?) : WcAnalyticsEvent()
    }
}