package com.tangem.domain.walletconnect

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent

/**
 * Events of the connections screen once it lists MCP agents next to dApps: they replace their [WcAnalyticEvents]
 * counterparts while MCP is enabled.
 */
sealed class ConnectionsAnalyticEvents(event: String) : AnalyticsEvent(category = CATEGORY, event = event) {

    class ScreenOpened : ConnectionsAnalyticEvents(event = "Connections Screen Opened"), AppsFlyerIncludedEvent

    class ButtonDisconnectAll : ConnectionsAnalyticEvents(event = "Button - Disconnect All")

    private companion object {
        const val CATEGORY = "Connections"
    }
}