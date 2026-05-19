package com.tangem.feature.stories.impl

import com.tangem.domain.promo.models.StoryContentIds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal data class SlideConfig(
    val titleResId: Int,
    val subtitleResId: Int,
)

internal object StoriesSlideConfigs {

    fun getSlides(storyId: String): ImmutableList<SlideConfig> = when (storyId) {
        StoryContentIds.STORY_FIRST_TIME_SWAP.id -> swapSlides()
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
}