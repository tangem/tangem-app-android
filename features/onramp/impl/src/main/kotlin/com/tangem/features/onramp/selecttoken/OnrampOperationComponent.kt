package com.tangem.features.onramp.selecttoken

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.onramp.entity.OnrampOperation

/**
 * Base operation component
 *
 * @author Andrew Khokhlov on 22/10/2024
 */
internal interface OnrampOperationComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, OnrampOperationComponent>

    /**
     * Params
     *
     * @property operation    operation
     * @property userWalletId id of multi-currency wallet
     */
    data class Params(val operation: OnrampOperation, val userWalletId: UserWalletId)
}
