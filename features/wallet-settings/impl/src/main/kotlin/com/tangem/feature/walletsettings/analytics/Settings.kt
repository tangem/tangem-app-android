package com.tangem.feature.walletsettings.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class Settings(
    category: String = "Settings",
    event: String,
) : AnalyticsEvent(category, event) {

    data object ButtonCreateBackup : Settings(event = "Button - Create Backup")

    data object ButtonManageTokens : Settings(event = "Button - Manage Tokens")
}