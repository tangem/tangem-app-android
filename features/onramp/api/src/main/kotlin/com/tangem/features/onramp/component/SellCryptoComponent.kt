package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Sell crypto component
 *
[REDACTED_AUTHOR]
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