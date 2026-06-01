package com.tangem.features.commonfeatures.api.addfunds

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface AddFundsComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
    )

    interface Factory : ComponentFactory<Params, AddFundsComponent>
}