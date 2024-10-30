package com.tangem.features.onramp.selecttoken

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.onramp.entity.OnrampOperation

/**
 * Select token component
 *
 * @author Andrew Khokhlov on 22/10/2024
 */
internal interface OnrampSelectTokenComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<Params, OnrampSelectTokenComponent>

    /**
     * Params
     *
     * @property operation    operation
     * @property hasSearchBar flag that indicates if search bar should be shown
     * @property userWalletId id of multi-currency wallet
     */
    data class Params(
        val operation: OnrampOperation,
        val hasSearchBar: Boolean,
        val userWalletId: UserWalletId,
    )
}
