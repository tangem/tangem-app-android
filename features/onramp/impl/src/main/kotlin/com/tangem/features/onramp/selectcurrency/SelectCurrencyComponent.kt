package com.tangem.features.onramp.selectcurrency

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal interface SelectCurrencyComponent : ComposableBottomSheetComponent {

    data class Params(val onDismiss: () -> Unit)

    interface Factory : ComponentFactory<Params, SelectCurrencyComponent>
}