package com.tangem.features.swap

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface SwapComponent : ComposableContentComponent {

    data class Params(
        val currencyFrom: CryptoCurrency,
        val currencyTo: CryptoCurrency? = null,
        val userWalletId: UserWalletId,
        val isInitialReverseOrder: Boolean = false,
    )

    interface Factory : ComponentFactory<Params, SwapComponent>
}