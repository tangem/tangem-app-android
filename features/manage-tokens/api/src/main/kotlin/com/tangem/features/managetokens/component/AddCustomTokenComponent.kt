package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.wallets.models.UserWalletId

interface AddCustomTokenComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, AddCustomTokenComponent>
}