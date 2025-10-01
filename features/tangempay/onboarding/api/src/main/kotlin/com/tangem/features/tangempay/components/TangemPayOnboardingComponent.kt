package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface TangemPayOnboardingComponent : ComposableContentComponent {

    data class Params(
        val deeplink: String,
    )

    interface Factory : ComponentFactory<Params, TangemPayOnboardingComponent>
}