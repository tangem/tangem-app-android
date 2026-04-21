package com.tangem.core.ui.components.stories.model

import kotlinx.collections.immutable.ImmutableList

/**
 * Config for stories component
 *
 * @property stories            configuration list
 * @property isRestartable      indicates that stories progression restarts after last story
 * @property onClose             called when stories session ends (navigation + session analytics); receives watched story count
 * @property onNextStory         analytics-only; called when user manually taps to advance; receives 0-based story index
 * @property onStoryClosed       analytics-only; called on explicit close (back/button) before [onClose]; receives 0-based story index
 * @property onStoryPaused       analytics-only; called when user long-presses to pause; receives 0-based story index
 * @property onStoryAutoCompleted analytics-only; called when story timer auto-advances; receives 0-based story index
 */
interface StoriesContentConfig<T : StoryConfig> {
    val stories: ImmutableList<T>
    val isRestartable: Boolean
    val onClose: (Int) -> Unit
    val onNextStory: ((Int) -> Unit)? get() = null
    val onStoryClosed: ((Int) -> Unit)? get() = null
    val onStoryPaused: ((Int) -> Unit)? get() = null
    val onStoryAutoCompleted: ((Int) -> Unit)? get() = null
}

interface StoryConfig {
    val duration: Int
}