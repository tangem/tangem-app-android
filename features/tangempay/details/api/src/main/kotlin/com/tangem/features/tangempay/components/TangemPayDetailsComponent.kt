package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface TangemPayDetailsComponent : ComposableContentComponent {
    data class Params(val customerWalletAddress: String, val cardNumberEnd: String)
    interface Factory : ComponentFactory<Params, TangemPayDetailsComponent>
}