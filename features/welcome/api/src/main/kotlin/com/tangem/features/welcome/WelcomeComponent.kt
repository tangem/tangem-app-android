package com.tangem.features.welcome

import com.tangem.common.routing.entity.SerializableIntent
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WelcomeComponent : ComposableContentComponent {

    data class Params(
        val intent: SerializableIntent?,
    )

    interface Factory : ComponentFactory<Params, WelcomeComponent>
}