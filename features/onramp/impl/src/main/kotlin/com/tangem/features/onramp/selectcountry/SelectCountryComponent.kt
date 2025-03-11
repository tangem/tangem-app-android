package com.tangem.features.onramp.selectcountry

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.tokens.model.CryptoCurrency

internal interface SelectCountryComponent : ComposableBottomSheetComponent {

    data class Params(val cryptoCurrency: CryptoCurrency, val onDismiss: (Boolean) -> Unit)

    interface Factory : ComponentFactory<Params, SelectCountryComponent>
}