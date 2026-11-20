package com.tangem.features.allservices.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AllServicesComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Unit, AllServicesComponent>
}