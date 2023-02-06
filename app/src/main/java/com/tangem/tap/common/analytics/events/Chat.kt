package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent

/**
* [REDACTED_AUTHOR]
 */
sealed class Chat(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Chat", event, params) {

    class ScreenOpened : Chat("Chat Screen Opened")
}
