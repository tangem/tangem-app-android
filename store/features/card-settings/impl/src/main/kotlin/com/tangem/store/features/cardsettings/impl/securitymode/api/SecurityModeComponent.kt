package com.tangem.store.features.cardsettings.impl.securitymode.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

internal interface SecurityModeComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
    )

    interface Factory : ComponentFactory<Params, SecurityModeComponent>
}