package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.OneTimeAnalyticsEvent

sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    class ButtonManageTokens : Settings(event = "Button - Manage Tokens")

    class ColdWalletAdded(
        source: AnalyticsParam.ScreensSources?,
    ) : Settings(
        event = "Cold Wallet Added",
        params = mapOf(AnalyticsParam.SOURCE to (source?.value ?: "Unknown")),
    ), OneTimeAnalyticsEvent, AppsFlyerIncludedEvent {
        override val oneTimeEventId: String = id
    }
}