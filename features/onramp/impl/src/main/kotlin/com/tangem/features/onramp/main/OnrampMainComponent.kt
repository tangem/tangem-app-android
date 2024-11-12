package com.tangem.features.onramp.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

internal interface OnrampMainComponent : ComposableContentComponent {

    class Params(val currency: String, val openSettings: () -> Unit)

    interface Factory : ComponentFactory<Params, OnrampMainComponent>
}
