package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface TangemPayDetailsComponent : ComposableContentComponent {
    @Suppress("EmptyDefaultConstructor") // Will add params in Next PRs
    class Params()
    interface Factory : ComponentFactory<Params, TangemPayDetailsComponent>
}