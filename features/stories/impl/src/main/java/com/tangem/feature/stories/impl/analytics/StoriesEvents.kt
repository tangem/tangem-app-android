package com.tangem.feature.stories.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.WATCHED
import com.tangem.domain.stories.models.StoryContentIds

internal sealed class StoriesEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent("Stories", event, params) {

    data class SwapStories(
        val source: String,
        val watchCount: String,
    ) : StoriesEvents(
        event = "Swap Story",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            WATCHED to watchCount,
        ),
    )

    data class YieldPromo(
        val source: String,
        val watchCount: String,
    ) : StoriesEvents(
        event = "Yield Promo",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            WATCHED to watchCount,
        ),
    )

    data class PolymarketOnboarding(
        val source: String,
        val watchCount: String,
    ) : StoriesEvents(
        event = "Polymarket Onboarding",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            WATCHED to watchCount,
        ),
    )

    companion object {

        fun storyShown(storyId: String, source: String, watchCount: String): StoriesEvents? =
            when (StoryContentIds.entries.firstOrNull { it.id == storyId }) {
                StoryContentIds.STORY_POLYMARKET_ONBOARDING -> PolymarketOnboarding(source, watchCount)
                StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO -> YieldPromo(source, watchCount)
                StoryContentIds.STORY_FIRST_TIME_SWAP -> SwapStories(source, watchCount)
                null -> null
            }
    }
}