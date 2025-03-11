package com.tangem.features.onramp.settings

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrency

internal interface OnrampSettingsComponent : ComposableContentComponent {

    data class Params(val cryptoCurrency: CryptoCurrency, val onBack: () -> Unit)

    interface Factory : ComponentFactory<Params, OnrampSettingsComponent>
}