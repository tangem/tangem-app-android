package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Buy crypto component
 *
[REDACTED_AUTHOR]
 */
interface BuyCryptoComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, BuyCryptoComponent>

    /**
     * Params
     *
     * @property userWalletId user wallet id
     */
    data class Params(val userWalletId: UserWalletId)
}