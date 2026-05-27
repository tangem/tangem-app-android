package com.tangem.core.analytics.models.event

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class AssetsDiscoveryAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Token Sync", event = event, params = params) {

    class SyncStarted : AssetsDiscoveryAnalyticsEvent(event = "Sync Started")

    class SyncCompleted : AssetsDiscoveryAnalyticsEvent(event = "Sync Completed")

    class ButtonManageTokens : AssetsDiscoveryAnalyticsEvent(event = "Button - Manage Tokens")

    class ButtonCloseBanner : AssetsDiscoveryAnalyticsEvent(event = "Button - Close Banner")
}