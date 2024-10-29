package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Sell crypto component
 *
 * @author Andrew Khokhlov on 28/10/2024
 */
interface SellCryptoComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, SellCryptoComponent>

    /**
     * Params
     *
     * @property userWalletId user wallet id
     */
    data class Params(val userWalletId: UserWalletId)
}
