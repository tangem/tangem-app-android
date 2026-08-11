package com.tangem.features.hotwallet

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AddExistingWalletComponent : ComposableContentComponent {

    data class Params(
        val mode: AppRoute.AddExistingWallet.Mode = AppRoute.AddExistingWallet.Mode.RecoveryPhrase,
    )

    interface Factory : ComponentFactory<Params, AddExistingWalletComponent>
}