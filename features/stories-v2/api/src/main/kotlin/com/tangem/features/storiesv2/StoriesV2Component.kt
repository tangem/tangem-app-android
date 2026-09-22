package com.tangem.features.storiesv2

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/**
 * Full-screen story player. Owns playback, progress, pause, haptics and analytics; performs no network requests and
 * reads no feature flags. Every decision that leaves the player goes back to the host: action taps through
 * [Params.onAction], terminal outcomes through [Params.onResult].
 */
interface StoriesV2Component : ComposableContentComponent {

    /**
     * @property source   entry point the story was opened from, reported to analytics
     * @property onAction invoked when an action button is tapped, with playback already paused on the tapped
     *   slide. The story has not ended: the host performs the action and settles the tap through the handle —
     *   [StoriesV2PendingAction.resume] puts the viewer back on the same slide, [StoriesV2PendingAction.end]
     *   ends the story. A tap on a `close`-target action never arrives here; the player resolves it itself
     *   into [StoriesV2Result.Dismissed].
     * @property onResult invoked exactly once, when the story ends. An action tap alone does not end it — see
     *   [onAction].
     */
    data class Params(
        val type: StoryV2Type,
        val source: StoryV2Source,
        val onAction: (StoriesV2PendingAction) -> Unit,
        val onResult: (StoriesV2Result) -> Unit,
    )

    interface Factory : ComponentFactory<Params, StoriesV2Component>
}