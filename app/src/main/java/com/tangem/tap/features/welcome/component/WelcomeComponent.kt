package com.tangem.tap.features.welcome.component

import com.tangem.common.routing.entity.InitScreenLaunchMode
import com.tangem.common.routing.entity.SerializableIntent
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WelcomeComponent : ComposableContentComponent {

    data class Params(
        val launchMode: InitScreenLaunchMode,
        val intent: SerializableIntent?,
    )

    interface Factory : ComponentFactory<Params, WelcomeComponent>
}