package com.tangem.features.hotwallet

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface WalletHardwareBackupComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val source: AppRoute.WalletHardwareBackup.Source,
    )

    interface Factory : ComponentFactory<Params, WalletHardwareBackupComponent>
}