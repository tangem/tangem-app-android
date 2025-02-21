package com.tangem.common.ui.swapStoriesScreen

import com.tangem.core.ui.components.stories.inner.STORY_DURATION
import com.tangem.core.ui.components.stories.model.StoriesContentConfig
import com.tangem.core.ui.components.stories.model.StoryConfig
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

sealed class SwapStoriesUM {

    data object Empty : SwapStoriesUM()

    data class Content(
        override val stories: ImmutableList<Config>,
        override val onClose: (Int) -> Unit,
    ) : SwapStoriesUM(), StoriesContentConfig<Content.Config> {
        override val isRestartable: Boolean = false

        data class Config(
            val imageUrl: String,
            val title: TextReference,
            val subtitle: TextReference,
        ) : StoryConfig {
            override val duration: Int = STORY_DURATION
        }
    }
}