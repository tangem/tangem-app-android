package com.tangem.feature.stories.impl

import com.tangem.domain.stories.models.StoryContentIds
import com.tangem.core.res.R as CoreResR
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SlideConfig(
    val titleResId: Int,
    val subtitleResId: Int,
)

internal object StoriesSlideConfigs {

    fun getSlides(storyId: String): ImmutableList<SlideConfig> = when (storyId) {
        StoryContentIds.STORY_FIRST_TIME_SWAP.id -> swapSlides()
        StoryContentIds.STORY_FIRST_TIME_YIELD_PROMO.id -> yieldPromoSlides()
        else -> persistentListOf()
    }

    private fun swapSlides(): ImmutableList<SlideConfig> = persistentListOf(
        SlideConfig(
            com.tangem.core.res.R.string.swap_story_first_title_v2,
            com.tangem.core.res.R.string.swap_story_first_subtitle_v2,
        ),
        SlideConfig(
            com.tangem.core.res.R.string.swap_story_second_title_v2,
            com.tangem.core.res.R.string.swap_story_second_subtitle_v2,
        ),
        SlideConfig(
            com.tangem.core.res.R.string.swap_story_third_title_v2,
            com.tangem.core.res.R.string.swap_story_third_subtitle_v2,
        ),
        SlideConfig(
            com.tangem.core.res.R.string.swap_story_forth_title_v2,
            com.tangem.core.res.R.string.swap_story_forth_subtitle_v2,
        ),
    )

    private fun yieldPromoSlides(): ImmutableList<SlideConfig> = persistentListOf(
        SlideConfig(
            CoreResR.string.yield_apy_boost_story_first_title,
            CoreResR.string.yield_apy_boost_story_first_subtitle,
        ),
        SlideConfig(
            CoreResR.string.yield_apy_boost_story_second_title,
            CoreResR.string.yield_apy_boost_story_second_subtitle,
        ),
        SlideConfig(
            CoreResR.string.yield_apy_boost_story_third_title,
            CoreResR.string.yield_apy_boost_story_third_subtitle,
        ),
        SlideConfig(
            CoreResR.string.yield_apy_boost_story_fourth_title,
            CoreResR.string.yield_apy_boost_story_fourth_subtitle,
        ),
    )
}