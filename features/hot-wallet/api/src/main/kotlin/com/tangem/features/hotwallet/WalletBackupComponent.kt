package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface WalletBackupComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val isColdWalletOptionShown: Boolean,
    )

    interface Factory : ComponentFactory<Params, WalletBackupComponent>
}