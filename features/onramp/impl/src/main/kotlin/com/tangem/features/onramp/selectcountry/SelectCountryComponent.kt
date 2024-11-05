package com.tangem.features.onramp.selectcountry

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent

internal interface SelectCountryComponent : ComposableBottomSheetComponent {

    data class Params(val onDismiss: () -> Unit)

    interface Factory : ComponentFactory<Params, SelectCountryComponent>
}