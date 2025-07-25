package com.tangem.features.swap

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface SwapComponent : ComposableContentComponent {

    data class Params(
        val currencyFrom: CryptoCurrency,
        val currencyTo: CryptoCurrency? = null,
        val userWalletId: UserWalletId,
        val isInitialReverseOrder: Boolean = false,
        val screenSource: String,
    )

    interface Factory : ComponentFactory<Params, SwapComponent>
}