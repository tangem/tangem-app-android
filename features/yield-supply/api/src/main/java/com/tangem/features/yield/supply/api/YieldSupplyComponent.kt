package com.tangem.features.yield.supply.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface YieldSupplyComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val handleNavigation: Boolean? = null,
    )

    interface Factory : ComponentFactory<Params, YieldSupplyComponent>
}