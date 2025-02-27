package com.tangem.feature.swap.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.WATCHED

sealed class StoriesEvents(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Stories", event, params) {

    data class SwapStories(
        val source: String,
        val watchCount: String,
    ) : StoriesEvents(
        event = "Swap Stories",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            WATCHED to watchCount,
        ),
    )

    data class Error(
        val type: String,
    ) : StoriesEvents(
        event = "Error",
        params = mapOf(
            AnalyticsParam.TYPE to type,
        ),
    )
}