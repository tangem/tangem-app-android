package com.tangem.feature.walletsettings.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.domain.models.wallet.UserWalletId

interface RenameWalletComponent : ComposableDialogComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val currentName: String,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, RenameWalletComponent>
}