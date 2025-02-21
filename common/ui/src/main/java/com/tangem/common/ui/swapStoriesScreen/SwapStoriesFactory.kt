package com.tangem.common.ui.swapStoriesScreen

import com.tangem.common.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.promo.models.StoryContent
import kotlinx.collections.immutable.persistentListOf

object SwapStoriesFactory {

    // WARNING! Be careful with indices. Temporary solution.
    // Use all data from v1/stories api (image url, title, subtitle)
    @Suppress("MagicNumber")
    fun createStoriesState(swapStory: StoryContent, onStoriesClose: (Int) -> Unit): SwapStoriesUM {
        val storyOrderedImageUrls = swapStory.getImageUrls()
        if (storyOrderedImageUrls.size != 5) return SwapStoriesUM.Empty

        return SwapStoriesUM.Content(
            stories = persistentListOf(
                SwapStoriesUM.Content.Config(
                    imageUrl = storyOrderedImageUrls[0],
                    title = resourceReference(R.string.swap_story_first_title),
                    subtitle = resourceReference(R.string.swap_story_first_subtitle),
                ),
                SwapStoriesUM.Content.Config(
                    imageUrl = storyOrderedImageUrls[1],
                    title = resourceReference(R.string.swap_story_second_title),
                    subtitle = resourceReference(R.string.swap_story_second_subtitle),
                ),
                SwapStoriesUM.Content.Config(
                    imageUrl = storyOrderedImageUrls[2],
                    title = resourceReference(R.string.swap_story_third_title),
                    subtitle = resourceReference(R.string.swap_story_third_subtitle),
                ),
                SwapStoriesUM.Content.Config(
                    imageUrl = storyOrderedImageUrls[3],
                    title = resourceReference(R.string.swap_story_forth_title),
                    subtitle = resourceReference(R.string.swap_story_forth_subtitle),
                ),
                SwapStoriesUM.Content.Config(
                    imageUrl = storyOrderedImageUrls[4],
                    title = resourceReference(R.string.swap_story_fifth_title),
                    subtitle = resourceReference(R.string.swap_story_fifth_subtitle),
                ),
            ),
            onClose = onStoriesClose,
        )
    }
}