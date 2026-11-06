package com.tangem.features.feed.model.feed.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class FeedAnalyticsEvent(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Markets", event = event, params = params) {

    class TokenSearchedClicked : FeedAnalyticsEvent(event = "Token Searched Clicked")
}