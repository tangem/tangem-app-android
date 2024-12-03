package com.tangem.features.onramp.providers

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import com.tangem.domain.tokens.model.CryptoCurrency

internal interface SelectProviderComponent : ComposableBottomSheetComponent {

    data class Params(
        val onProviderClick: (OnrampProviderWithQuote.Data, Boolean) -> Unit,
        val onDismiss: () -> Unit,
        val selectedPaymentMethod: OnrampPaymentMethod,
        val cryptoCurrency: CryptoCurrency,
    )

    interface Factory : ComponentFactory<Params, SelectProviderComponent>
}