package com.tangem.features.yield.supply.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface YieldSupplyPromoComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val apy: String,
        val isPromoEnabled: Boolean = false,
    )

    interface Factory : ComponentFactory<Params, YieldSupplyPromoComponent>
}