package com.tangem.features.polymarket.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface PolymarketComponent : ComposableContentComponent {

    /**
     * @property userWalletId wallet the caller picked, e.g. the per-wallet row on the wallet screen. `null`
     *  means the caller did not pick a wallet and the feature must ask.
     */
    data class Params(
        val userWalletId: UserWalletId?,
    )

    interface Factory : ComponentFactory<Params, PolymarketComponent>
}