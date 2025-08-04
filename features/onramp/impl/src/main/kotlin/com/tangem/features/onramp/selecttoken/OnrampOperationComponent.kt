package com.tangem.features.onramp.selecttoken

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Base operation component
 *
[REDACTED_AUTHOR]
 */
internal interface OnrampOperationComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, OnrampOperationComponent>

    /**
     * Params
     *
     * @property userWalletId id of multi-currency wallet
     */
    sealed interface Params {

        val userWalletId: UserWalletId

        data class Buy(override val userWalletId: UserWalletId) : Params

        data class Sell(override val userWalletId: UserWalletId) : Params
    }
}