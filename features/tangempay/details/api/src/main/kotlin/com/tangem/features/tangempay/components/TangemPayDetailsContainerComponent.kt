package com.tangem.features.tangempay.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayDetailsConfig

interface TangemPayDetailsContainerComponent : ComposableContentComponent {
    data class Params(val userWalletId: UserWalletId, val config: TangemPayDetailsConfig)
    interface Factory : ComponentFactory<Params, TangemPayDetailsContainerComponent>
}