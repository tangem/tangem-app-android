package com.tangem.features.send.v2.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface FeeSelectorBlockComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, FeeSelectorBlockComponent>
}