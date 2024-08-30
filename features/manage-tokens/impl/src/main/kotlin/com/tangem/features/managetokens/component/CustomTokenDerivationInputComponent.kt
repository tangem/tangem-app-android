package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableDialogComponent
import com.tangem.domain.wallets.models.UserWalletId

internal interface CustomTokenDerivationInputComponent : ComposableDialogComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val onConfirm: (String) -> Unit,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, CustomTokenDerivationInputComponent>
}