package com.tangem.features.onramp.main

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrency

internal interface OnrampMainComponent : ComposableContentComponent {

    data class Params(val cryptoCurrency: CryptoCurrency, val openSettings: () -> Unit)

    interface Factory : ComponentFactory<Params, OnrampMainComponent>
}