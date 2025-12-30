package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface CreateMobileWalletComponent : ComposableContentComponent {
    data class Params(
        val source: String,
    )

    interface Factory : ComponentFactory<Params, CreateMobileWalletComponent>
}