package com.tangem.features.managetokens.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface ManageTokensComponent : ComposableContentComponent {

    data class Params(val mode: Mode)

    sealed class Mode {
        abstract val showToolbar: Boolean

        data class ReadOnly(override val showToolbar: Boolean) : Mode()
        data class Manage(
            override val showToolbar: Boolean,
            val userWalletId: UserWalletId,
            val onSaved: () -> Unit,
            val forceShowSaveButton: Boolean = false,
        ) : Mode()
    }

    interface Factory : ComponentFactory<Params, ManageTokensComponent>
}
