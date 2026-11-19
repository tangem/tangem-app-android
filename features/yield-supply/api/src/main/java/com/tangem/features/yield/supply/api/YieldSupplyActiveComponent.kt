package com.tangem.features.yield.supply.api

import com.tangem.common.routing.AppRoute.YieldSupplyEntry.AccountKind
import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface YieldSupplyActiveComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val account: AccountKind,
    )

    interface Factory : ComponentFactory<Params, YieldSupplyActiveComponent>
}