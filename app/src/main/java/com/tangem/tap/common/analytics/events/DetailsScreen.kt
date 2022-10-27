package com.tangem.tap.common.analytics.events

/**
* [REDACTED_AUTHOR]
 */
sealed class DetailsScreen(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Details Screen", event, params) {

    class ScreenOpened : DetailsScreen("Details Screen Opened")
}
