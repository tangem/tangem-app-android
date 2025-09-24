package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface TangemPayOnboardingComponent : ComposableContentComponent {

    sealed class Params {
        data class Deeplink(
            val deeplink: String,
        ) : Params()

        object ContinueOnboarding : Params()
    }

    interface Factory : ComponentFactory<Params, TangemPayOnboardingComponent>
}