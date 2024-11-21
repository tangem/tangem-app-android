package com.tangem.features.onramp.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.tokens.model.CryptoCurrency

interface OnrampComponent : ComposableContentComponent {

    data class Params(val cryptoCurrency: CryptoCurrency)

    interface Factory : ComponentFactory<Params, OnrampComponent>
}