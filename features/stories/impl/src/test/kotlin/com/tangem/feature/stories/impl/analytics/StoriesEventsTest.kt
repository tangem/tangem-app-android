package com.tangem.feature.stories.impl.analytics

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.stories.models.StoryContentIds
import org.junit.jupiter.api.Test

internal class StoriesEventsTest {

    @Test
    fun `GIVEN the polymarket onboarding story WHEN it is watched THEN its own event is reported`() {
        // Act
        val actual = StoriesEvents.storyShown(
            storyId = StoryContentIds.STORY_POLYMARKET_ONBOARDING.id,
            source = "Predictions",
            watchCount = "4",
        )

        // Assert
        assertThat(actual).isEqualTo(
            StoriesEvents.PolymarketOnboardingStory(source = "Predictions", watchCount = "4"),
        )
        assertThat(actual.event).isEqualTo("Polymarket Onboarding Story")
    }

    @Test
    fun `GIVEN the yield promo story WHEN it is watched THEN its own event is reported`() {
        // Act
        val actual = StoriesEvents.storyShown(
            storyId = StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id,
            source = "TokenDetails",
            watchCount = "3",
        )

        // Assert
        assertThat(actual).isEqualTo(
            StoriesEvents.YieldPromoStory(source = "TokenDetails", watchCount = "3"),
        )
        assertThat(actual.event).isEqualTo("Yield Promo Story")
    }

    @Test
    fun `GIVEN the swap story WHEN it is watched THEN the swap event is reported`() {
        // Act
        val actual = StoriesEvents.storyShown(
            storyId = StoryContentIds.STORY_FIRST_TIME_SWAP.id,
            source = "Main",
            watchCount = "1",
        )

        // Assert
        assertThat(actual).isEqualTo(StoriesEvents.SwapStories(source = "Main", watchCount = "1"))
    }
}