package com.tangem.features.details.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface DetailsComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, DetailsComponent>

    data class Params(
        val userWalletId: UserWalletId,
    )
}
