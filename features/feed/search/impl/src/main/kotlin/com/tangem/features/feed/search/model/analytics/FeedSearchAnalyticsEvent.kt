package com.tangem.features.feed.search.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE

internal sealed class FeedSearchAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Search", event = event, params = params) {

    data class SearchScreenOpened(
        private val screensSource: String,
    ) : FeedSearchAnalyticsEvent(
        event = "Search Screen Opened",
        params = mapOf(SOURCE to screensSource),
    )

    class SearchStarted : FeedSearchAnalyticsEvent(event = "Search Started")
}