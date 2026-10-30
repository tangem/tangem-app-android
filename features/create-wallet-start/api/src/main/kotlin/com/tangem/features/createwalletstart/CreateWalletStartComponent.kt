package com.tangem.features.createwalletstart

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface CreateWalletStartComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Unit, CreateWalletStartComponent>
}