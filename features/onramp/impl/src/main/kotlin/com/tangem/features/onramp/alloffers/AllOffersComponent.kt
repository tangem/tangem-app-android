package com.tangem.features.onramp.alloffers

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.onramp.model.OnrampProviderWithQuote

internal interface AllOffersComponent : ComposableBottomSheetComponent {

    data class Params(
        val userWallet: UserWallet,
        val cryptoCurrency: CryptoCurrency,
        val onDismiss: () -> Unit,
        val openRedirectPage: (quote: OnrampProviderWithQuote.Data) -> Unit,
        val amountCurrencyCode: String,
    )

    interface Factory : ComponentFactory<Params, AllOffersComponent>
}