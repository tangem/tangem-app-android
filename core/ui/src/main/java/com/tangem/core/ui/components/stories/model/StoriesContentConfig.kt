package com.tangem.core.ui.components.stories.model

import kotlinx.collections.immutable.ImmutableList

/**
 * Config for stories component
 *
 * @property stories        configuration list
 * @property isRestartable   indicates than stories progressions starts
 */
interface StoriesContentConfig<T : StoryConfig> {
    val stories: ImmutableList<T>
    val isRestartable: Boolean
    val onClose: (Int) -> Unit
}

interface StoryConfig {
    val duration: Int
}