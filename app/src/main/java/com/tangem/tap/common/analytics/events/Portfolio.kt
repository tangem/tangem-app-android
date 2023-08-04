package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent

/**
* [REDACTED_AUTHOR]
 */

sealed class Portfolio(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Portfolio", event, params) {

    class Refreshed : Portfolio("Refreshed")
    class ButtonManageTokens : Portfolio("Button - Manage Tokens")
    class TokenTapped : Portfolio("Token is Tapped")
}
