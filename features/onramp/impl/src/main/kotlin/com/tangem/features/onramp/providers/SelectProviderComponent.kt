package com.tangem.features.onramp.providers

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProviderWithQuote

internal interface SelectProviderComponent : ComposableBottomSheetComponent {

    data class Params(
        val onProviderClick: (OnrampProviderWithQuote.Data) -> Unit,
        val onDismiss: () -> Unit,
        val selectedPaymentMethod: OnrampPaymentMethod,
    )

    interface Factory : ComponentFactory<Params, SelectProviderComponent>
}