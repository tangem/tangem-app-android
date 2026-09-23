package com.tangem.features.collectibles.api

import com.tangem.common.routing.entity.CollectiblesStartRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface CollectiblesEntryComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val startRoute: CollectiblesStartRoute,
    )

    interface Factory : ComponentFactory<Params, CollectiblesEntryComponent>
}