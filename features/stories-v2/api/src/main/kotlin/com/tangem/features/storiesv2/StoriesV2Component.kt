package com.tangem.features.storiesv2

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/**
 * Full-screen story player. Owns playback, progress, pause, haptics and analytics; performs no network requests and
 * reads no feature flags, and hands every terminal decision back through [Params.onResult].
 */
interface StoriesV2Component : ComposableContentComponent {

    /**
     * @property source   entry point the story was opened from, reported to analytics
     * @property onResult invoked exactly once, when the story ends
     */
    data class Params(
        val type: StoryV2Type,
        val source: StoryV2Source,
        val onResult: (StoriesV2Result) -> Unit,
    )

    interface Factory : ComponentFactory<Params, StoriesV2Component>
}