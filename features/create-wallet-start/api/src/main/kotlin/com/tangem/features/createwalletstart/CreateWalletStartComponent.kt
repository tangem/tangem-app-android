package com.tangem.features.createwalletstart

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface CreateWalletStartComponent : ComposableContentComponent {

    data class Params(
        val mode: Mode,
    )

    enum class Mode {
        ColdWallet,
        HotWallet,
    }

    interface Factory : ComponentFactory<Params, CreateWalletStartComponent>
}