package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface OnrampComponent : ComposableContentComponent {

    data class Params(val currency: String)

    interface Factory : ComponentFactory<Params, OnrampComponent>
}
