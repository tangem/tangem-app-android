package com.tangem.feature.usedesk.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam

internal sealed class UsedeskAnalyticsEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Support", event = event, params = params) {

    class ChatScreenOpened(source: AnalyticsParam.ScreensSources) : UsedeskAnalyticsEvents(
        event = "Chat Screen Opened",
        params = mapOf(AnalyticsParam.SOURCE to source.value),
    )

    class ChatScreenError : UsedeskAnalyticsEvents(event = "Chat Screen Error")

    class ChatScreenClosed : UsedeskAnalyticsEvents(event = "Chat Screen Closed")
}