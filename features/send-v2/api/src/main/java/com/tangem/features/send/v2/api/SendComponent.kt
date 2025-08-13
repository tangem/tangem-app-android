package com.tangem.features.send.v2.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId

interface SendComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val currency: CryptoCurrency,
        val transactionId: String? = null,
        val amount: String? = null,
        val tag: String? = null,
        val destinationAddress: String? = null,
        val callback: ModelCallback? = null,
    )

    interface Factory : ComponentFactory<Params, SendComponent>

    interface ModelCallback {
        fun onConvertToAnotherToken(lastAmount: String)
    }
}