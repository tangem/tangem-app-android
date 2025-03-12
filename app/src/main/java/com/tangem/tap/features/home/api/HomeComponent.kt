package com.tangem.tap.features.home.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface HomeComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, HomeComponent>
}