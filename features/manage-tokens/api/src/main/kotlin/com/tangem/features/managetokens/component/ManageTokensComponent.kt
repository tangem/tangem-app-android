package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface ManageTokensComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId?,
        val source: ManageTokensSource,
    )

    interface Factory : ComponentFactory<Params, ManageTokensComponent>
}