package com.tangem.feature.walletsettings.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class Settings(
    category: String = "Settings",
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent(category, event, params, error) {

    class ButtonCreateBackup : Settings(event = "Button - Create Backup")
}
