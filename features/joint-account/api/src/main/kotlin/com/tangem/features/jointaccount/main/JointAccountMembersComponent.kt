package com.tangem.features.jointaccount.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.wallet.UserWalletId

interface JointAccountMembersComponent : ComposableContentComponent {
    /**
     * @property cryptoAccountId `null` only in navigation state saved before the field existed: the screen then
     * falls back to the wallet's first joint account
     */
    data class Params(
        val userWalletId: UserWalletId,
        val cryptoAccountId: String?,
    )

    interface Factory : ComponentFactory<Params, JointAccountMembersComponent>
}