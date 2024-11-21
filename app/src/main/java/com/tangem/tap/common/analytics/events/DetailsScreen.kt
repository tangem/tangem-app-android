package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.EventValue

/**
[REDACTED_AUTHOR]
 */
sealed class DetailsScreen(
    event: String,
    params: Map<String, EventValue> = mapOf(),
) : AnalyticsEvent("Details Screen", event, params) {

    class ScreenOpened : DetailsScreen("Details Screen Opened")
}