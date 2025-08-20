package com.tangem.features.welcome

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface WelcomeComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, WelcomeComponent>
}