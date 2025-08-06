package com.tangem.features.account

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.wallet.UserWalletId

interface AccountCreateEditComponent : ComposableContentComponent {
    interface Factory : ComponentFactory<Params, AccountCreateEditComponent>

    sealed interface Params {

        data class Create(
            val userWalletId: UserWalletId,
        ) : Params

        data class Edit(
            val account: Account,
        ) : Params
    }
}