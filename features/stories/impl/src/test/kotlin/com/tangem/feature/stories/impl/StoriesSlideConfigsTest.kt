package com.tangem.feature.stories.impl

import com.google.common.truth.Truth.assertThat
import com.tangem.core.res.R
import com.tangem.domain.stories.models.StoryContentIds
import org.junit.jupiter.api.Test

internal class StoriesSlideConfigsTest {

    @Test
    fun `GIVEN the polymarket onboarding story WHEN slides requested THEN four slides are returned`() {
        // Act
        val actual = StoriesSlideConfigs.getSlides(StoryContentIds.STORY_POLYMARKET_ONBOARDING.id)

        // Assert
        assertThat(actual).containsExactly(
            SlideConfig(
                R.string.prediction_onboarding_story_first_title,
                R.string.prediction_onboarding_story_first_subtitle,
            ),
            SlideConfig(
                R.string.prediction_onboarding_story_second_title,
                R.string.prediction_onboarding_story_second_subtitle,
            ),
            SlideConfig(
                R.string.prediction_onboarding_story_third_title,
                R.string.prediction_onboarding_story_third_subtitle,
            ),
            SlideConfig(
                R.string.prediction_onboarding_story_fourth_title,
                R.string.prediction_onboarding_story_fourth_subtitle,
            ),
        ).inOrder()
    }

    @Test
    fun `GIVEN an unknown story WHEN slides requested THEN nothing is returned`() {
        // Act
        val actual = StoriesSlideConfigs.getSlides(storyId = "unknown-story")

        // Assert
        assertThat(actual).isEmpty()
    }
}