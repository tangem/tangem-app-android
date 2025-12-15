package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    class ButtonManageTokens : Settings(event = "Button - Manage Tokens")
}