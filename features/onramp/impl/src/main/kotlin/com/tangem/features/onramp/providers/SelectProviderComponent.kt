package com.tangem.features.onramp.providers

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.features.onramp.providers.entity.SelectProviderResult

internal interface SelectProviderComponent : ComposableBottomSheetComponent {

    data class Params(
        val onProviderClick: (result: SelectProviderResult, isBestRate: Boolean) -> Unit,
        val onDismiss: () -> Unit,
        val selectedPaymentMethod: OnrampPaymentMethod,
        val selectedProviderId: String,
        val cryptoCurrency: CryptoCurrency,
    )

    interface Factory : ComponentFactory<Params, SelectProviderComponent>
}