package com.tangem.features.walletconnect.components

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface WalletConnectEntryComponent : ComposableContentComponent {
    data class Params(val userWalletId: UserWalletId)
    interface Factory : ComponentFactory<Params, WalletConnectEntryComponent>
}
