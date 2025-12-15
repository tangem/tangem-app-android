package com.tangem.features.hotwallet

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface CreateWalletBackupComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val isUpgradeFlow: Boolean,
        val shouldSetAccessCode: Boolean,
        val analyticsSource: String,
        val analyticsAction: String,
    )

    interface Factory : ComponentFactory<Params, CreateWalletBackupComponent>
}