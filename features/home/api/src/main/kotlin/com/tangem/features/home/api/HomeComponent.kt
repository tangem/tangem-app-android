package com.tangem.features.home.api

import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface HomeComponent : ComposableContentComponent {

    data class Params(
        val launchMode: InitScreenLaunchMode = InitScreenLaunchMode.Standard,
    )

    interface Factory : ComponentFactory<Params, HomeComponent> {
        override fun create(context: AppComponentContext, params: Params): HomeComponent
    }
}