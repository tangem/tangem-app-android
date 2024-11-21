package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue

/**
[REDACTED_AUTHOR]
 */
sealed class Chat(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent("Chat", event, params) {

    class ScreenOpened : Chat("Chat Screen Opened")
}