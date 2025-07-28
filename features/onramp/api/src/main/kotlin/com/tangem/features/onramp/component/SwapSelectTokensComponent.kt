package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Swap select tokens component
 *
[REDACTED_AUTHOR]
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