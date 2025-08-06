package com.tangem.features.account

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent

interface AccountCreateEditComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Params, AccountCreateEditComponent>

    sealed interface Params {

        data object Create : Params
        data object Edit : Params
    }
}