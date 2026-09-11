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

    data class PolymarketOnboardingStory(
        val source: String,
        val watchCount: String,
    ) : StoriesEvents(
        event = "Polymarket Onboarding Story",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            WATCHED to watchCount,
        ),
    )

    companion object {

        fun storyShown(storyId: String, source: String, watchCount: String): StoriesEvents = when (storyId) {
            StoryContentIds.STORY_POLYMARKET_ONBOARDING.id -> PolymarketOnboardingStory(source, watchCount)
            else -> SwapStories(source, watchCount)
        }
    }
}