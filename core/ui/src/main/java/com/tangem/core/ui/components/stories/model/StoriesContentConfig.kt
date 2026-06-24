package com.tangem.core.ui.components.stories.model

import kotlinx.collections.immutable.ImmutableList

/**
 * Config for stories component
 *
 * @property stories         configuration list
 * @property isRestartable   indicates than stories progressions starts
 * @property isCloseButtonVisible whether the top-right close button (and back handler) is shown.
 *  Set `false` for non-closable stories (e.g. a root intro screen); [onClose] is then irrelevant.
 * @property onClose         invoked with the number of watched stories when the user closes them.
 *  Only meaningful when [isCloseButtonVisible] is `true`; defaults to a no-op for non-closable stories.
 */
interface StoriesContentConfig<T : StoryConfig> {
    val stories: ImmutableList<T>
    val isRestartable: Boolean
    val isCloseButtonVisible: Boolean get() = true
    val onClose: (Int) -> Unit get() = {}
}

interface StoryConfig {
    val duration: Int
}