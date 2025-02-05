package com.tangem.feature.swap.models

import com.tangem.core.ui.components.stories.model.StoriesContentConfig
import com.tangem.core.ui.components.stories.model.StoryConfig
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

data class SwapStoriesContentConfig(
    override val stories: ImmutableList<SwapStoryConfig>,
    override val onClose: () -> Unit,
) : StoriesContentConfig<SwapStoryConfig> {
    override val isRestartable: Boolean = false
}

data class SwapStoryConfig(
    val imageUrl: String,
    val title: TextReference,
    val subtitle: TextReference,
) : StoryConfig {
    override val duration: Int = 2000
}