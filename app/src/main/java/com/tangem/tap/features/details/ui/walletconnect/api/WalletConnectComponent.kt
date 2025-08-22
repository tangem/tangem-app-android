package com.tangem.tap.features.details.ui.walletconnect.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface WalletConnectComponent : ComposableContentComponent {
    data class Params(val userWalletId: UserWalletId)
    interface Factory : ComponentFactory<Params, WalletConnectComponent>
}