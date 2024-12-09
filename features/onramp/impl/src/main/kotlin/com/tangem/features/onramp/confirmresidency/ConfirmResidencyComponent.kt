package com.tangem.features.onramp.confirmresidency

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.domain.onramp.model.OnrampCountry
import com.tangem.domain.tokens.model.CryptoCurrency

internal interface ConfirmResidencyComponent : ComposableBottomSheetComponent {

    data class Params(
        val cryptoCurrency: CryptoCurrency,
        val country: OnrampCountry,
        val onDismiss: (OnrampCountry) -> Unit,
    )

    interface Factory : ComponentFactory<Params, ConfirmResidencyComponent>
}
