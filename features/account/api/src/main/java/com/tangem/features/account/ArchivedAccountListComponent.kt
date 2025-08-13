package com.tangem.features.account

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface ArchivedAccountListComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Params, ArchivedAccountListComponent>

    data class Params(val userWalletId: UserWalletId)
}