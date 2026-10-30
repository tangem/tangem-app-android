package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface TangemPayOnboardingSuccessComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Unit, TangemPayOnboardingSuccessComponent>
}