package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface OnrampComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, OnrampComponent>
}