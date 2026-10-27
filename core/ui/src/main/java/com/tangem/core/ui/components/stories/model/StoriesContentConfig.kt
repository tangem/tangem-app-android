package com.tangem.core.ui.components.stories.model

import kotlinx.collections.immutable.ImmutableList

/**
 * Config for stories component
 *
 * @property stories configuration list
 * @property onClose invoked with the number of watched stories when the user closes them
 */
interface StoriesContentConfig<T : StoryConfig> {
    val stories: ImmutableList<T>
    val onClose: (Int) -> Unit
}

interface StoryConfig {
    val duration: Int
}