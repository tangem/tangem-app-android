package com.tangem.features.onramp.redirect

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWalletId

internal interface OnrampRedirectComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val onBack: () -> Unit,
        val cryptoCurrency: CryptoCurrency,
        val onrampProviderWithQuote: OnrampProviderWithQuote.Data,
    )

    interface Factory : ComponentFactory<Params, OnrampRedirectComponent>
}
