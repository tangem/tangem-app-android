package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface ManageTokensComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
    )

    interface Factory : ComponentFactory<Params, ManageTokensComponent>
}
