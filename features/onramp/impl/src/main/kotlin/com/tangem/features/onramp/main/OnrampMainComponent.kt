package com.tangem.features.onramp.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.onramp.model.OnrampSource
import com.tangem.domain.wallets.models.UserWalletId

internal interface OnrampMainComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cryptoCurrency: CryptoCurrency,
        val source: OnrampSource,
        val openSettings: () -> Unit,
        val openRedirectPage: (quote: OnrampProviderWithQuote.Data) -> Unit,
    )

    interface Factory : ComponentFactory<Params, OnrampMainComponent>
}