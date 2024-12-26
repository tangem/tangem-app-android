package com.tangem.features.stories.api.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface StoriesComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Params, StoriesComponent>

    /**
     * Params
     */
    data object Params
}