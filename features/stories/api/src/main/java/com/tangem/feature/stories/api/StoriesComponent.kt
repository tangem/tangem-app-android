package com.tangem.feature.stories.api

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface StoriesComponent : ComposableContentComponent {

    data class Params(
        val storyId: String,
        val nextScreen: AppRoute,
        val screenSource: String,
    )

    interface Factory : ComponentFactory<Params, StoriesComponent>
}