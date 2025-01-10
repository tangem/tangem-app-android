package com.tangem.feature.swap.models

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.stories.model.StoriesContentConfig
import com.tangem.core.ui.components.stories.model.StoryConfig
import kotlinx.collections.immutable.ImmutableList

data class SwapStoriesContentConfig(
    override val stories: ImmutableList<SwapStoryConfig>,
    override val onClose: () -> Unit,
) : StoriesContentConfig<SwapStoryConfig> {
    override val isRestartable: Boolean = false
}

data class SwapStoryConfig(
    @DrawableRes val imageRes: Int,
) : StoryConfig {
    override val duration: Int = 2000
}