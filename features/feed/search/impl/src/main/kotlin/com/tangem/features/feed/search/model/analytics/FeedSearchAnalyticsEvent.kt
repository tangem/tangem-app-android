package com.tangem.features.feed.search.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.SOURCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

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

    data class HintClicked(
        private val hint: String,
    ) : FeedSearchAnalyticsEvent(
        event = "Hint Clicked",
        params = mapOf("Text" to hint),
    )

    data class RecentItemClicked(
        private val tokenSymbol: String,
    ) : FeedSearchAnalyticsEvent(
        event = "Recent Item Clicked",
        params = mapOf(TOKEN_PARAM to tokenSymbol),
    )

    class ButtonClearHistoryClick : FeedSearchAnalyticsEvent(event = "Button - Clear History")
}