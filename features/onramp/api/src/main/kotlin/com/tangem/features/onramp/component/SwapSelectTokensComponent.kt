package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Swap select tokens component
 *
 * @author Andrew Khokhlov on 30/10/2024
 */
interface SwapSelectTokensComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, SwapSelectTokensComponent>

    /**
     * Params
     *
     * @property userWalletId user wallet id
     */
    data class Params(val userWalletId: UserWalletId)
}
