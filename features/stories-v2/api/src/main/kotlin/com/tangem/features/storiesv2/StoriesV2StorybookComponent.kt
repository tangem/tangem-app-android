package com.tangem.features.storiesv2

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

/**
 * Tester-only showcase of the story player. Behind the same component boundary as the player itself, so the demo
 * runs the real dependency graph, model lifecycle and playback rather than a look-alike that can drift from them.
 */
interface StoriesV2StorybookComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, StoriesV2StorybookComponent>
}