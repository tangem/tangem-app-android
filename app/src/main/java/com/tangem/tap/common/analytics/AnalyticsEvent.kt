package com.tangem.tap.common.analytics

enum class AnalyticsEvent(val event: String) {

    VIEW_STORY_1("view_story_1"),
    VIEW_STORY_2("view_story_2"),
    VIEW_STORY_3("view_story_3"),
    VIEW_STORY_4("view_story_4"),
    VIEW_STORY_5("view_story_5"),
    VIEW_STORY_6("view_story_6"),
    TOKEN_LIST_TAPPED("token_list_tapped"),
    SEARCH_TOKEN("search_token"),
    BUY_BOTTOM_TAPPED("buy_bottom_tapped"),
    FIRST_SCREEN_SCAN_CARD_TAPPED("first_screen_scan_card_tapped"),
    FIRST_SCAN("first_scan"),
    ACCESS_CODE_TAPPED("access_code_tapped"),
    FIRST_SCREEN_ACCESS_CODE_ENTERED("first_screen_access_code_entered"),
    SECOND_SCAN("second_scan"),
    SUPPORT_TAPPED("support_tapped"),
    TRY_AGAIN_TAPPED("try_again_tapped"),
    SESSION_EXPIRED("session_expired"),
    ACCESS_CODE_INCORRECT("access_code_incorrect"),
    NEW_CODE_ENTERED("new_code_entered"),
    NEW_CODE_CONFIRMED("new_code_confirmed"),
    CARD_CODE_RESET("card_code_reset"),

    CREATE_WALLET_TAPPED("create_wallet_tapped"),
    BACKUP_TAPPED("backup_tapped"),
    BACKUP_LATER_TAPPED("backup_later_tapped"),
    FIRST_CARD_SCAN("first_card_scan"),
    ADD_BACKUP_CARD("add_backup_card"),
    BACKUP_FINISH("backup_finish"),
    CREATE_ACCESS_CODE("create_access_code"),
    ACCESS_CODE_ENTERED("access_code_entered"),
    ACCESS_CODE_CONFIRM("access_code_confirm"),
    CARD_CODE_SAVE("card_code_save"),
    BACKUP_CARD_SAVE("backup_card_save"),
    ONBOARDING_SUCCESS("onboarding_success"),


    MAIN_PAGE_ENTER("main_page_enter"),
    MAIN_PAGE_SWIPE("main_page_swipe"),
    CURRENCY_TYPE_TAPPED("currency_type_tapped"),
    CURRENCY_CHANGED("currency_changed"),
    SETTINGS_TAPPED("settings_tapped"),
    MANAGE_TOKENS_TAPPED("manage_tokens_tapped"),
    TOKEN_TAPPED("token_tapped"),
    SCAN_CARD_TAPPED("scan_card_tapped"),

    CHAT_TAPPED("chat_tapped"),
    WC_TAPPED("wc_tapped"),
    FACTORY_RESET_TAPPED("factory_reset_tapped"),
    FACTORY_RESET_SUCCESS("factory_reset_success"),
    CREATE_BACKUP_TAPPED("create_backup_tapped"),
    MAKE_COMMENT("make_comment"),
    COMMENT_SENT("comment_sent"),

    WALLET_CONNECT_SUCCESS_RESPONSE("wallet_connect_success_response"),
    WALLET_CONNECT_INVALID_REQUEST("wallet_connect_invalid_request"),
    WALLET_CONNECT_NEW_SESSION("wallet_connect_new_session"),
    WALLET_CONNECT_SESSION_DISCONNECTED("wallet_connect_session_disconnected"),


    TOKEN_SEARCH("token_search"),
    TOKEN_SWITCH_ON("token_switch_on"),
    TOKEN_SWITCH_OFF("token_switch_off"),
    TOKEN_LIST_SAVE("token_list_save"),
    CUSTOM_TOKEN_ADD("custom_token_add"),
    CUSTOM_TOKEN_SAVE("custom_token_save"),


    REMOVE_TOKEN("remove_token"),
    COPY_ADDRESS("copy_address"),
    SHARE_ADDRESS("share_address"),
    CHECK_ADDRESS("check_address"),
    BUY_TOKEN_TAPPED("buy_token_tapped"),
    P2P_INSTRUCTION_TAPPED("p2p_instruction_tapped"),
    TRANSACTION_IS_SENT("transaction_is_sent"),
    SEND_TOKEN_TAPPED("send_token_tapped"),


    CARD_IS_SCANNED("card_is_scanned"),
    READY_TO_SCAN("ready_to_scan"),
    DEMO_MODE_ACTIVATED("demo_mode_activated"),

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

    FIRST_SCREEN_TOKEN_NAME("first_screen_token_name"),
    FIRST_SCAN_SUCCESS("first_scan_success"),
    ACCESS_CODE_ENTERED_SUCCESS("access_code_entered_success"),
    SECOND_SCAN_SUCCESS("second_scan_success"),
    CURRENCY("Currency"),
    TOKEN_SEARCH_TOKEN_NAME("token_search_token_name"),
    TOKEN_SWITCH_ON_TOKEN_NAME("token_switch_on_token_name"),
    TOKEN_SWITCH_OFF_TOKEN_NAME("token_switch_off_token_name"),
    BUY_TOKEN_TAPPED_TOKEN_NAME("buy_token_tapped_token_name"),
    TYPE("type"),


    BLOCKCHAIN("blockchain"),
    CARD_ID("cardId"),
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
