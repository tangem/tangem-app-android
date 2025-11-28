package com.tangem.domain.wallets.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    data object ButtonCreateBackup : Settings(event = "Button - Create Backup")

    data object ButtonManageTokens : Settings(event = "Button - Manage Tokens")
}