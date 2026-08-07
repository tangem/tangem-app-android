package com.tangem.features.polymarket.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface PolymarketComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
    )

    interface Factory : ComponentFactory<Params, PolymarketComponent>
}