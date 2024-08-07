package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
sealed class Token(
    category: String,
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent(category, event, params, error) {

    sealed class Receive(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Receive", event, params) {

        class ScreenOpened : Receive("Receive Screen Opened")
        class ButtonCopyAddress : Receive("Button - Copy Address")
        class ButtonShareAddress : Receive("Button - Share Address")
    }

    sealed class Topup(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Topup", event, params) {

        class ScreenOpened : Topup("Top Up Screen Opened")
        class P2PScreenOpened : Topup("P2P Screen Opened")
    }

    sealed class Withdraw(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : Token("Token / Withdraw", event, params) {

        class ScreenOpened : Withdraw("Withdraw Screen Opened")
    }
}