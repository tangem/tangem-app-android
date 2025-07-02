package com.tangem.features.feeselector.api.component

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.appcurrency.model.AppCurrency

interface FeeSelectorComponent : ComposableContentComponent, ComposableBottomSheetComponent {
    data class Params(val appCurrency: AppCurrency)

    interface Factory : ComponentFactory<Params, FeeSelectorComponent>
}