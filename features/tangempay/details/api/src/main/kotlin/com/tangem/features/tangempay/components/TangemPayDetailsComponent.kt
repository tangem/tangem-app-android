package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.pay.TangemPayDetailsConfig

interface TangemPayDetailsComponent : ComposableContentComponent {
    data class Params(val config: TangemPayDetailsConfig)
    interface Factory : ComponentFactory<Params, TangemPayDetailsComponent>
}