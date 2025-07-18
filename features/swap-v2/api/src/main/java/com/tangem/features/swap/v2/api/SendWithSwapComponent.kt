package com.tangem.features.swap.v2.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

interface SendWithSwapComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val callback: ModelCallback? = null,
    )

    interface Factory : ComponentFactory<Params, SendWithSwapComponent>

    interface ModelCallback {
        fun onCloseSwap(lastAmount: String)
    }
}