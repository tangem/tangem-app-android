package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface ManageTokensComponent : ComposableContentComponent {

    data class Params(
        val mode: ManageTokensMode,
        val source: ManageTokensSource,
    ) {
        constructor(userWalletId: UserWalletId?, source: ManageTokensSource) : this(
            source = source,
            mode = userWalletId
                ?.let { ManageTokensMode.Wallet(userWalletId) }
                ?: ManageTokensMode.None,
        )
    }

    interface Factory : ComponentFactory<Params, ManageTokensComponent>
}