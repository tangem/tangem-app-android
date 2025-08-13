package com.tangem.feature.walletsettings.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface WalletSettingsComponent : ComposableContentComponent {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : ComponentFactory<Params, WalletSettingsComponent>
}