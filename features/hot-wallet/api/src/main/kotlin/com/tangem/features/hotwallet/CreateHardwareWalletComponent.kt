package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface CreateHardwareWalletComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, CreateHardwareWalletComponent>
}