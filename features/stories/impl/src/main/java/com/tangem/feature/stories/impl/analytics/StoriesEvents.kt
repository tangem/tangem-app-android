package com.tangem.feature.stories.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.WATCHED

internal sealed class StoriesEvents(
    category: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category, event, params) {

    data class SwapStories(
        private val source: String,
        private val watchCount: String,
    ) : StoriesEvents(
        category = "Stories",
        event = "Swap Stories",
        params = mapOf(
            AnalyticsParam.SOURCE to source,
            WATCHED to watchCount,
        ),
    )

    data class CloseStories(private val storyNumber: Int) : StoriesEvents(
        category = "Swap Story $storyNumber",
        event = "Close stories",
    )

    data class NextStory(private val storyNumber: Int) : StoriesEvents(
        category = "Swap Story $storyNumber",
        event = "Next story",
    )

    data class StoryPaused(private val storyNumber: Int) : StoriesEvents(
        category = "Swap Story $storyNumber",
        event = "Story paused",
    )

    data class AutoplayCompleted(private val storyNumber: Int) : StoriesEvents(
        category = "Swap Story $storyNumber",
        event = "Autoplay completed",
    )
}