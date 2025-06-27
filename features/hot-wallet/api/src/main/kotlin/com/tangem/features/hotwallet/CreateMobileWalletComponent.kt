package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface CreateMobileWalletComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Unit, CreateMobileWalletComponent>
}