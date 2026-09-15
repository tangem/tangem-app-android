package com.tangem.features.moneyaccount.details.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.wallet.UserWalletId

interface MoneyAccountMainComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val virtualAccountOnramp: VirtualAccountOnramp.Available,
    )

    interface Factory : ComponentFactory<Params, MoneyAccountMainComponent>
}