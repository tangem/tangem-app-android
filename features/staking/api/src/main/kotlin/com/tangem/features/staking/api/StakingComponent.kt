package com.tangem.features.staking.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface StakingComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrencyId: CryptoCurrency.ID,
        val yieldId: String,
    )

    interface Factory : ComponentFactory<Params, StakingComponent>
}