package com.tangem.features.details.component

import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface DetailsComponent : ComposableContentComponent {

    interface Factory : ComponentFactory<DetailsComponent, Params> {

        override fun create(context: AppComponentContext, params: Params): DetailsComponent
    }

    data class Params(
        val selectedUserWalletId: UserWalletId,
    )
}